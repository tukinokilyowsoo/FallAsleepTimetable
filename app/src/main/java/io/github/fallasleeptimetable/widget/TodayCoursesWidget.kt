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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.fallasleeptimetable.MainActivity
import io.github.fallasleeptimetable.ui.formatMinutes
import java.time.LocalDate
import java.time.LocalTime

class TodayCoursesWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent {
            val width = LocalSize.current.width
            val dayCount = when {
                width < 240.dp -> 1
                width < 380.dp -> 2
                else -> 3
            }
            TodayCoursesContent(context, data, dayCount)
        }
    }
}

class TodayCoursesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCoursesWidget()
}

@Composable
private fun TodayCoursesContent(
    context: Context,
    data: WidgetData?,
    dayCount: Int,
) {
    val palette = widgetPalette(context)
    val today = LocalDate.now()
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.background))
            .cornerRadius(20.dp)
            .padding(10.dp)
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
        } else {
            val actualWeek = data.actualWeek(today)
            val todaySessions = if (actualWeek in 1..data.term.totalWeeks) {
                data.sessionsForWeek(actualWeek)
                    .filter { it.dayOfWeek == today.dayOfWeek.value }
                    .sortedBy { it.startPeriod }
            } else {
                emptyList()
            }
            val remainingToday = todaySessions.filter { it.endMinute > nowMinutes }
            val allDoneToday = todaySessions.isNotEmpty() && remainingToday.isEmpty()

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                repeat(dayCount) { index ->
                    val date = today.plusDays(index.toLong())
                    val week = data.actualWeek(date)
                    val sessions = if (index == 0) {
                        remainingToday
                    } else if (week in 1..data.term.totalWeeks) {
                        data.sessionsForWeek(week)
                            .filter { it.dayOfWeek == date.dayOfWeek.value }
                            .sortedBy { it.startPeriod }
                    } else {
                        emptyList()
                    }

                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(horizontal = 4.dp),
                    ) {
                        Text(
                            text = "${dayLabel(index)} ${date.monthValue}/${date.dayOfMonth}",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(palette.text),
                            ),
                            maxLines = 1,
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))

                        if (index == 0 && allDoneToday) {
                            Text(
                                text = "你已免费，辛苦了喵😋",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = ColorProvider(palette.secondaryText),
                                ),
                                maxLines = 2,
                            )
                        } else if (sessions.isEmpty()) {
                            Text(
                                text = "没有课",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = ColorProvider(palette.secondaryText),
                                ),
                            )
                        } else {
                            sessions.forEach { session ->
                                val colorIndex = ((session.colorIndex % 6) + 6) % 6
                                val container = palette.containers[colorIndex]
                                val contentColor = palette.onContainers[colorIndex]

                                Column(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(ColorProvider(container))
                                        .cornerRadius(10.dp)
                                        .padding(5.dp),
                                ) {
                                    Text(
                                        text = "${formatMinutes(session.startMinute)} ${session.name}",
                                        style = TextStyle(
                                            fontSize = if (dayCount == 1) 12.sp else 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ColorProvider(contentColor),
                                        ),
                                        maxLines = if (dayCount == 1) 2 else 3,
                                    )
                                    val detail = listOfNotNull(
                                        session.room,
                                        session.teacher,
                                    ).joinToString(" · ")
                                    if (detail.isNotBlank()) {
                                        Text(
                                            text = detail,
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                color = ColorProvider(contentColor),
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dayLabel(index: Int): String = when (index) {
    0 -> "今天"
    1 -> "明天"
    else -> "后天"
}
