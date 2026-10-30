package io.github.fallasleeptimetable.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate

class WeekScheduleWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent {
            val size = LocalSize.current
            WeekScheduleContent(context, data, size.width, size.height)
        }
    }
}

class WeekScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekScheduleWidget()
}

class WeekPrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        changeWeek(context, -1)
    }
}

class WeekNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        changeWeek(context, 1)
    }
}

private suspend fun changeWeek(context: Context, delta: Int) {
    val data = WidgetDataLoader.load(context) ?: return
    val actual = data.actualWeek(LocalDate.now()).coerceIn(1, data.term.totalWeeks)
    val current = (actual + WidgetWeekState.offset(context))
        .coerceIn(1, data.term.totalWeeks)
    val target = (current + delta).coerceIn(1, data.term.totalWeeks)
    WidgetWeekState.setOffset(context, target - actual)
    WeekScheduleWidget().updateAll(context)
}

@Composable
private fun WeekScheduleContent(
    context: Context,
    data: WidgetData?,
    width: Dp,
    height: Dp,
) {
    val palette = widgetPalette(context)
    val today = LocalDate.now()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.background))
            .padding(8.dp),
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
            val actualWeek = data.actualWeek(today).coerceIn(1, data.term.totalWeeks)
            val week = (actualWeek + WidgetWeekState.offset(context))
                .coerceIn(1, data.term.totalWeeks)
            val sessions = data.sessionsForWeek(week)
            val days = buildList {
                addAll(1..5)
                if (data.term.showSaturday) add(6)
                if (data.term.showSunday) add(7)
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = androidx.glance.layout.Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<WeekPrevAction>())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(palette.text),
                    ),
                )
                Text(
                    text = "第 $week 周",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(palette.text),
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = "›",
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<WeekNextAction>())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(palette.text),
                    ),
                )
            }

            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                Spacer(modifier = GlanceModifier.width(26.dp))
                days.forEach { day ->
                    val isToday = week == actualWeek && day == today.dayOfWeek.value
                    Text(
                        text = dayShort(day),
                        modifier = GlanceModifier.defaultWeight().padding(horizontal = 1.dp),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = ColorProvider(
                                if (isToday) palette.text else palette.secondaryText,
                            ),
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }

            val periods = data.term.periodsPerDay.coerceAtLeast(1)
            val gridHeight = (height - 64.dp).coerceAtLeast(96.dp)
            val unit = (gridHeight / periods).coerceIn(12.dp, 36.dp)

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.width(26.dp)) {
                    repeat(periods) { index ->
                        Text(
                            text = "${index + 1}",
                            modifier = GlanceModifier.height(unit),
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = ColorProvider(palette.secondaryText),
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                        )
                    }
                }

                days.forEach { day ->
                    val daySessions = sessions
                        .filter { it.dayOfWeek == day }
                        .sortedBy { it.startPeriod }
                    val (laneOf, laneCount) = assignLanes(daySessions)

                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(unit * periods),
                    ) {
                        repeat(laneCount) { lane ->
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                var cursor = 1
                                daySessions
                                    .filter { laneOf[it.sessionId] == lane }
                                    .forEach { session ->
                                        if (session.startPeriod > cursor) {
                                            Spacer(
                                                modifier = GlanceModifier.height(
                                                    unit * (session.startPeriod - cursor),
                                                ),
                                            )
                                        }
                                        val colorIndex =
                                            ((session.colorIndex % 6) + 6) % 6
                                        Box(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .height(unit * session.periodCount)
                                                .padding(1.dp)
                                                .background(
                                                    ColorProvider(
                                                        palette.containers[colorIndex],
                                                    ),
                                                )
                                                .padding(2.dp),
                                        ) {
                                            Text(
                                                text = session.name,
                                                style = TextStyle(
                                                    fontSize = 8.sp,
                                                    color = ColorProvider(
                                                        palette.onContainers[colorIndex],
                                                    ),
                                                ),
                                                maxLines = 6,
                                            )
                                        }
                                        cursor = session.startPeriod + session.periodCount
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun assignLanes(
    sessions: List<WidgetSession>,
): Pair<Map<Long, Int>, Int> {
    val laneEnds = mutableListOf<Int>()
    val laneOf = mutableMapOf<Long, Int>()

    sessions.sortedWith(compareBy({ it.startPeriod }, { it.sessionId }))
        .forEach { session ->
            val end = session.startPeriod + session.periodCount - 1
            val free = laneEnds.indexOfFirst { it < session.startPeriod }
            if (free == -1) {
                laneOf[session.sessionId] = laneEnds.size
                laneEnds.add(end)
            } else {
                laneOf[session.sessionId] = free
                laneEnds[free] = end
            }
        }
    return laneOf to laneEnds.size.coerceAtLeast(1)
}

private fun dayShort(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    else -> "日"
}
