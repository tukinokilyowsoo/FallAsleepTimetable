package io.github.fallasleeptimetable.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.fallasleeptimetable.MainActivity
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.formatMinutes
import io.github.fallasleeptimetable.ui.periodLabel
import java.time.LocalDate
import java.time.LocalTime

class TodayTimelineWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = WidgetDataLoader.load(context, appWidgetId)
        provideContent {
            TodayTimelineContent(context, data)
        }
    }
}

class TodayTimelineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTimelineWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetConfigStore.delete(context, it) }
    }
}

@Composable
private fun TodayTimelineContent(context: Context, data: WidgetData?) {
    val palette = widgetPalette(context)
    val today = LocalDate.now()
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
    val height = LocalSize.current.height
    val maxRows = when {
        height < 150.dp -> 2
        height < 200.dp -> 3
        height < 260.dp -> 4
        else -> 6
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.background))
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        if (data == null) {
            Text(
                text = "打开 FallAsleep 课程表添加课表",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(palette.secondaryText),
                ),
            )
            return@Column
        }

        val week = data.actualWeek(today)
        val inTerm = week in 1..data.term.totalWeeks
        val sessions = if (inTerm) {
            data.sessionsForWeek(week)
                .filter { it.dayOfWeek == today.dayOfWeek.value }
                .sortedBy { it.startPeriod }
        } else {
            emptyList()
        }

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "${today.monthValue}/${today.dayOfMonth} " +
                    dayLabel(today.dayOfWeek.value),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(palette.text),
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Text(
                text = if (inTerm) "第 ${data.actualWeek(today)} 周" else "假期",
                style = TextStyle(
                    fontSize = 9.sp,
                    color = ColorProvider(palette.secondaryText),
                ),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))

        if (sessions.isEmpty()) {
            Text(
                text = if (inTerm) "今天没有课" else "不在学期内",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(palette.secondaryText),
                ),
            )
            return@Column
        }

        sessions.take(maxRows).forEach { session ->
            val finished = nowMinutes >= session.endMinute
            val current = nowMinutes >= session.startMinute && nowMinutes < session.endMinute
            val colorIndex = ((session.colorIndex % 6) + 6) % 6
            val dotColor = palette.containers[colorIndex]
            val titleColor = if (finished && !current) {
                palette.secondaryText
            } else {
                palette.text
            }
            val metaColor = palette.secondaryText
            val rowModifier = if (current) {
                GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(palette.surface))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            } else {
                GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            }

            Row(modifier = rowModifier) {
                Column(modifier = GlanceModifier.width(44.dp)) {
                    Text(
                        text = formatMinutes(session.startMinute),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(if (current) palette.text else metaColor),
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = formatMinutes(session.endMinute),
                        style = TextStyle(
                            fontSize = 8.sp,
                            color = ColorProvider(metaColor),
                        ),
                        maxLines = 1,
                    )
                }

                Column(modifier = GlanceModifier.width(8.dp)) {
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(ColorProvider(dotColor))
                            .cornerRadius(4.dp),
                    ) {}
                    Row(modifier = GlanceModifier.width(8.dp)) {
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Box(
                            modifier = GlanceModifier
                                .width(2.dp)
                                .height(22.dp)
                                .background(ColorProvider(palette.outline)),
                        ) {}
                    }
                }

                Column(modifier = GlanceModifier.defaultWeight().padding(start = 6.dp)) {
                    Text(
                        text = session.name,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(titleColor),
                        ),
                        maxLines = 1,
                    )
                    val meta = buildList {
                        add(periodLabel(session.startPeriod, session.periodCount))
                        session.room
                            ?.takeIf { data.config.showRoom && it.isNotBlank() }
                            ?.let { add(it) }
                        session.teacher
                            ?.takeIf { data.config.showTeacher && it.isNotBlank() }
                            ?.let { add(it) }
                        if (current) add("上课中") else if (finished) add("已结束")
                    }.joinToString(" · ")
                    Text(
                        text = meta,
                        style = TextStyle(
                            fontSize = 8.sp,
                            color = ColorProvider(metaColor),
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        if (sessions.size > maxRows) {
            Text(
                text = "还有 ${sessions.size - maxRows} 节课",
                style = TextStyle(
                    fontSize = 8.sp,
                    color = ColorProvider(palette.secondaryText),
                ),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = 2.dp),
            )
        }
    }
}
