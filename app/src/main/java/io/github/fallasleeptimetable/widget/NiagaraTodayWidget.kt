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
 * 面向 Niagara Launcher 等支持自由缩放启动器的透明今日课程组件。
 *
 * 极简待办式布局：课程名是主信息，用大号字横向铺满剩余宽度；
 * 第 n 节、时间、教室作为次要信息，小号字右对齐。
 * 字号由组件实际宽高的像素测量决定，不依赖启动器的字体设置。
 * 背景透明，文字按壁纸亮度自动取黑或白。
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
    val name: String,
    val meta: String,
)

private data class NiagaraLayout(
    val nameFontSize: TextUnit,
    val metaFontSize: TextUnit,
    val nameWidth: Dp,
    val metaWidth: Dp,
    val gap: Dp,
    val showMeta: Boolean,
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
    val maxRows = NiagaraWidgetSetting.rows(context).coerceIn(1, 8)
    val verticalPadding = 2.dp
    val startPadding = 8.dp
    val endPadding = 40.dp
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
                    fontSize = fitEmptyTextSize(
                        contentWidth = contentWidth,
                        rowHeight = availableHeight / maxRows.coerceAtLeast(1).toFloat(),
                        fontScale = fontScale,
                    ),
                    color = ColorProvider(textColors.secondary),
                ),
                maxLines = 1,
            )
            return@Column
        }

        val rows = remaining.map { session ->
            NiagaraRow(
                name = session.name,
                meta = buildString {
                    append("第").append(session.startPeriod)
                    if (session.periodCount > 1) {
                        append('-').append(session.startPeriod + session.periodCount - 1)
                    }
                    append("节 ")
                    append(formatMinutes(session.startMinute))
                    append('-')
                    append(formatMinutes(session.endMinute))
                    session.room?.takeIf { it.isNotBlank() }?.let {
                        append("  ").append(it)
                    }
                },
            )
        }

        val (layout, visibleRows) = resolveLayout(
            rows = rows,
            maxRows = maxRows,
            contentWidth = contentWidth,
            availableHeight = availableHeight,
            fontScale = fontScale,
            requestedFontSp = NiagaraWidgetSetting.fontSize(context),
        )

        val rowHeight = availableHeight / visibleRows.toFloat()
        rows.take(minOf(visibleRows, rows.size)).forEach { row ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(rowHeight),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = row.name,
                    modifier = GlanceModifier.width(
                        if (layout.showMeta) layout.nameWidth else contentWidth,
                    ),
                    style = TextStyle(
                        fontSize = layout.nameFontSize,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(textColors.primary),
                    ),
                    maxLines = 1,
                )
                if (layout.showMeta) {
                    Spacer(modifier = GlanceModifier.width(layout.gap))
                    Text(
                        text = row.meta,
                        modifier = GlanceModifier.width(layout.metaWidth),
                        style = TextStyle(
                            fontSize = layout.metaFontSize,
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

private const val META_RATIO = 0.45f
private const val GAP_EM = 0.35f
private const val MAX_NAME_SP = 72f

/**
 * 自动模式：课程越少行越高、字越大；空间不足时减少行数。
 * 手动模式：先保证用户选择的字号，行数让位，必要时裁掉右侧说明文字。
 */
private fun resolveLayout(
    rows: List<NiagaraRow>,
    maxRows: Int,
    contentWidth: Dp,
    availableHeight: Dp,
    fontScale: Float,
    requestedFontSp: Int,
): Pair<NiagaraLayout, Int> {
    var visible = maxRows.coerceAtLeast(1)

    if (requestedFontSp <= NiagaraWidgetSetting.FONT_AUTO) {
        var layout = computeLayout(
            rows = rows.take(visible),
            contentWidth = contentWidth,
            rowHeight = availableHeight / visible.toFloat(),
            fontScale = fontScale,
        )
        while (visible > 1 && layout.nameFontSize.value < 11f) {
            visible--
            layout = computeLayout(
                rows = rows.take(visible),
                contentWidth = contentWidth,
                rowHeight = availableHeight / visible.toFloat(),
                fontScale = fontScale,
            )
        }
        return layout to visible
    }

    while (visible > 1) {
        val rowHeight = availableHeight / visible.toFloat()
        val maxNameByHeight = rowHeight.value * 0.78f / fontScale
        if (requestedFontSp.toFloat() <= maxNameByHeight) break
        visible--
    }
    val rowHeight = availableHeight / visible.toFloat()
    return computeManualLayout(
        rows = rows.take(visible),
        contentWidth = contentWidth,
        rowHeight = rowHeight,
        fontScale = fontScale,
        requestedSp = requestedFontSp,
    ) to visible
}

private fun computeManualLayout(
    rows: List<NiagaraRow>,
    contentWidth: Dp,
    rowHeight: Dp,
    fontScale: Float,
    requestedSp: Int,
): NiagaraLayout {
    val nameEm = rows.maxOfOrNull { textWidthEm(it.name, bold = true) } ?: 1f
    val metaEm = rows.maxOfOrNull { textWidthEm(it.meta, bold = false) } ?: 1f
    val maxNameByWidth = contentWidth.value / (nameEm * fontScale)
    val maxNameByHeight = rowHeight.value * 0.78f / fontScale
    val nameFont = min(
        requestedSp.toFloat(),
        min(maxNameByWidth, maxNameByHeight),
    ).coerceIn(8f, MAX_NAME_SP)

    val gapDp = GAP_EM * nameFont * fontScale
    val nameWidthDp = nameEm * nameFont * fontScale
    val roomForMeta = (contentWidth.value - nameWidthDp - gapDp).coerceAtLeast(0f)
    var metaFont = maxOf(requestedSp * META_RATIO, 8f)
    if (metaEm > 0f) {
        metaFont = min(metaFont, roomForMeta / (metaEm * fontScale))
    }
    val showMeta = metaEm > 0f && metaFont >= 8f && roomForMeta > 0f

    return NiagaraLayout(
        nameFontSize = nameFont.sp,
        metaFontSize = (if (showMeta) metaFont else 0f).sp,
        nameWidth = (if (showMeta) nameWidthDp else contentWidth.value).dp,
        metaWidth = (if (showMeta) metaEm * metaFont * fontScale else 0f).dp,
        gap = (if (showMeta) gapDp else 0f).dp,
        showMeta = showMeta,
    )
}

/**
 * 用实际字体测量最长课程名与最长说明文本，让两者在横向一起铺满组件。
 * 课程名字号为主，说明文本为其 0.45 倍；同时不超过行高。
 */
private fun computeLayout(
    rows: List<NiagaraRow>,
    contentWidth: Dp,
    rowHeight: Dp,
    fontScale: Float,
): NiagaraLayout {
    val nameEm = rows.maxOfOrNull { textWidthEm(it.name, bold = true) } ?: 1f
    val metaEm = rows.maxOfOrNull { textWidthEm(it.meta, bold = false) } ?: 1f
    val heightFit = (rowHeight.value * 0.70f) / fontScale

    var nameFont = min(
        (contentWidth.value / (nameEm + metaEm * META_RATIO + GAP_EM).coerceAtLeast(0.1f)) /
            fontScale * 0.97f,
        heightFit,
    ).coerceIn(9f, MAX_NAME_SP)
    var metaFont = maxOf(nameFont * META_RATIO, 8f)
    // 说明文字有 8sp 下限；把这一点算进去再迭代让整行仍能铺满。
    repeat(3) {
        val ratio = (metaFont / nameFont).coerceAtLeast(0.1f)
        val totalEm = nameEm + metaEm * ratio + GAP_EM
        nameFont = min(
            (contentWidth.value / totalEm) / fontScale * 0.97f,
            heightFit,
        ).coerceIn(9f, MAX_NAME_SP)
        metaFont = maxOf(nameFont * META_RATIO, 8f)
    }

    val ratio = (metaFont / nameFont).coerceAtLeast(0.1f)
    val totalEm = nameEm + metaEm * ratio + GAP_EM
    val unit = contentWidth.value / totalEm.coerceAtLeast(0.1f)
    return NiagaraLayout(
        nameFontSize = nameFont.sp,
        metaFontSize = metaFont.sp,
        nameWidth = (unit * nameEm).dp,
        metaWidth = (unit * metaEm * ratio).dp,
        gap = (unit * GAP_EM).dp,
        showMeta = true,
    )
}

private fun fitEmptyTextSize(
    contentWidth: Dp,
    rowHeight: Dp,
    fontScale: Float,
): TextUnit {
    val em = textWidthEm("今天的课已经结束", bold = false).coerceAtLeast(1f)
    val widthFit = (contentWidth.value / em) / fontScale * 0.95f
    val heightFit = (rowHeight.value * 0.70f) / fontScale
    return min(widthFit, heightFit).coerceIn(9f, MAX_NAME_SP).sp
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
