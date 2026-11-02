package io.github.fallasleeptimetable.widget

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.fallasleeptimetable.MainActivity
import io.github.fallasleeptimetable.ui.formatMinutes
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.min

/**
 * 面向 Niagara Launcher 等自由缩放启动器的透明今日课程组件。
 *
 * 只保留最重要、最短的三项信息：开始时间 / 课程名 / 教室。
 * 字号按“最长一行”实测并留 18% 安全余量，配合更小的 30sp 上限，
 * 确保在 Niagara 关闭「填充屏幕」、或右侧被字母索引条占用时也能完整显示。
 *
 * 高度只按当天剩余课程数分配，不再为未出现的课程预留空槽。
 */
class NiagaraTodayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = WidgetDataLoader.load(context, appWidgetId)
        val textColors = adaptiveWidgetTextColors(context)
        provideContent {
            val size = LocalSize.current
            NiagaraTodayContent(
                context = context,
                data = data,
                width = size.width,
                height = size.height,
                textColors = textColors,
            )
        }
    }
}

class NiagaraTodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NiagaraTodayWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetConfigStore.delete(context, it) }
    }
}

private data class NiagaraRow(
    val time: String,
    val name: String,
    val room: String,
)

private data class NiagaraLayout(
    val nameFontSize: TextUnit,
    val detailFontSize: TextUnit,
    val timeWidth: Dp,
    val nameWidth: Dp,
    val roomWidth: Dp,
    val gap: Dp,
    val showRoom: Boolean,
)

@Composable
private fun NiagaraTodayContent(
    context: Context,
    data: WidgetData?,
    width: Dp,
    height: Dp,
    textColors: WidgetTextColors,
) {
    val today = LocalDate.now()
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
    val maxRows = NiagaraWidgetSetting.rows(context)
        .coerceIn(NiagaraWidgetSetting.MIN_ROWS, NiagaraWidgetSetting.MAX_ROWS)
    val verticalPadding = 2.dp
    val startPadding = 8.dp
    // Niagara 的字母索引条会盖住组件右侧；留出比之前更大的安全区。
    val endPadding = 56.dp
    val availableHeight = (height - verticalPadding * 2).coerceAtLeast(14.dp)
    val contentWidth = (width - startPadding - endPadding).coerceAtLeast(40.dp)
    val fontScale = context.resources.configuration.fontScale.coerceAtLeast(0.5f)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                start = startPadding,
                top = verticalPadding,
                end = endPadding,
                bottom = verticalPadding,
            )
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        if (data == null) {
            Text(
                text = "打开 FallAsleep 课程表添加课表",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(textColors.secondary),
                ),
                maxLines = 1,
            )
            return@Column
        }

        val week = data.actualWeek(today)
        val todaySessions = if (week in 1..data.term.totalWeeks) {
            data.sessionsForWeek(week)
                .filter { it.dayOfWeek == today.dayOfWeek.value }
                .sortedBy { it.startPeriod }
        } else {
            emptyList()
        }
        val remaining = todaySessions.filter { it.endMinute > nowMinutes }

        if (remaining.isEmpty()) {
            Text(
                text = if (todaySessions.isEmpty()) "今天没有课" else "今天的课已经结束",
                style = TextStyle(
                    fontSize = fitEmptyTextSize(contentWidth, availableHeight, fontScale),
                    color = ColorProvider(textColors.secondary),
                ),
                maxLines = 1,
            )
            return@Column
        }

        fun toRow(session: WidgetSession) = NiagaraRow(
            time = formatMinutes(session.startMinute),
            name = session.name,
            room = session.room.orEmpty(),
        )

        // 字号用“当天全部课程”测量，而不是剩余课程；下课时字号不会忽大忽小。
        val metricRows = todaySessions.map(::toRow)
        val displayRows = remaining.take(maxRows).map(::toRow)
        val rowHeight = availableHeight / maxRows.toFloat()
        val layout = buildLayout(
            rows = metricRows,
            contentWidth = contentWidth,
            rowHeight = rowHeight,
            fontScale = fontScale,
            requestedFontSp = NiagaraWidgetSetting.fontSize(context),
        )

        // 固定显示 maxRows 行；剩余课程不足时用空行占位，保持行高与字号稳定。
        repeat(maxRows) { index ->
            val row = displayRows.getOrNull(index)
            if (row == null) {
                Spacer(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(rowHeight),
                )
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = row.time,
                        modifier = GlanceModifier.width(layout.timeWidth),
                        style = TextStyle(
                            fontSize = layout.detailFontSize,
                            color = ColorProvider(textColors.secondary),
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.width(layout.gap))
                    Text(
                        text = row.name,
                        modifier = GlanceModifier.width(layout.nameWidth),
                        style = TextStyle(
                            fontSize = layout.nameFontSize,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(textColors.primary),
                        ),
                        maxLines = 1,
                    )
                    if (layout.showRoom) {
                        Spacer(modifier = GlanceModifier.width(layout.gap))
                        Text(
                            text = row.room,
                            modifier = GlanceModifier.width(layout.roomWidth),
                            style = TextStyle(
                                fontSize = layout.detailFontSize,
                                color = ColorProvider(textColors.secondary),
                                textAlign = TextAlign.End,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private const val DETAIL_RATIO = 0.85f
private const val GAP_EM = 0.35f
private const val MAX_FONT_SP = 30f
private const val FIT_SAFETY = 0.82f
private const val MIN_DETAIL_SP = 8f

/**
 * 用实际字体测量三列的最长文本，求出能完整放下的课程名字号。
 * 说明文字取课程名的 0.85 倍；再整体乘 0.82 安全系数，宁可偏小也不裁切。
 */
private fun buildLayout(
    rows: List<NiagaraRow>,
    contentWidth: Dp,
    rowHeight: Dp,
    fontScale: Float,
    requestedFontSp: Int,
): NiagaraLayout {
    val timeEm = rows.maxOfOrNull { textWidthEm(it.time, bold = false) } ?: 0.1f
    val nameEm = rows.maxOfOrNull { textWidthEm(it.name, bold = true) } ?: 1f
    val roomEm = rows.maxOfOrNull { textWidthEm(it.room, bold = false) } ?: 0f
    val showRoom = roomEm > 0f
    val gapCount = if (showRoom) 2 else 1
    val heightFit = (rowHeight.value * 0.62f) / fontScale

    fun widthFitFor(font: Float): Float {
        val detailRatio = maxOf(DETAIL_RATIO, MIN_DETAIL_SP / font)
        val totalEm = timeEm * detailRatio + nameEm +
            roomEm * detailRatio + GAP_EM * gapCount
        return (contentWidth.value / totalEm.coerceAtLeast(0.1f)) /
            fontScale * FIT_SAFETY
    }

    // 先求自动可用的最大字号；手动设置只能把它再调小，不能撑破可用宽度。
    var nameFont = MAX_FONT_SP
    repeat(4) {
        nameFont = min(widthFitFor(nameFont), heightFit)
            .coerceIn(9f, MAX_FONT_SP)
    }
    if (requestedFontSp > NiagaraWidgetSetting.FONT_AUTO) {
        nameFont = min(nameFont, requestedFontSp.toFloat())
    }

    val detailRatio = maxOf(DETAIL_RATIO, MIN_DETAIL_SP / nameFont)
    val detailFont = nameFont * detailRatio
    val totalEm = timeEm * detailRatio + nameEm +
        roomEm * detailRatio + GAP_EM * gapCount
    val unit = contentWidth.value / totalEm.coerceAtLeast(0.1f)

    return NiagaraLayout(
        nameFontSize = nameFont.sp,
        detailFontSize = detailFont.sp,
        timeWidth = (unit * timeEm * detailRatio).dp,
        nameWidth = (unit * nameEm).dp,
        roomWidth = (unit * roomEm * detailRatio).dp,
        gap = (unit * GAP_EM).dp,
        showRoom = showRoom,
    )
}

private fun fitEmptyTextSize(
    contentWidth: Dp,
    height: Dp,
    fontScale: Float,
): TextUnit {
    val em = textWidthEm("今天的课已经结束", bold = false).coerceAtLeast(1f)
    val widthFit = (contentWidth.value / em) / fontScale * FIT_SAFETY
    val heightFit = (height.value * 0.55f) / fontScale
    return min(widthFit, heightFit).coerceIn(9f, MAX_FONT_SP).sp
}

/** 以 100px 为基准测量文字宽度，返回相对字号（em）宽度。 */
private fun textWidthEm(text: String, bold: Boolean): Float {
    if (text.isEmpty()) return 0f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.typeface = Typeface.create(
        Typeface.DEFAULT,
        if (bold) Typeface.BOLD else Typeface.NORMAL,
    )
    paint.textSize = REFERENCE_TEXT_SIZE
    return paint.measureText(text) / REFERENCE_TEXT_SIZE
}

private const val REFERENCE_TEXT_SIZE = 100f
