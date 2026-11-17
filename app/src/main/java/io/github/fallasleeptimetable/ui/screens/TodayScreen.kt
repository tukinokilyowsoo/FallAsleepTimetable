package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.domain.VisibleSession
import io.github.fallasleeptimetable.domain.visibleSessions
import io.github.fallasleeptimetable.ui.ScheduleUiState
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.formatMinutes
import io.github.fallasleeptimetable.ui.periodLabel
import io.github.fallasleeptimetable.ui.weekRangeLabel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val todayDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: ScheduleUiState,
    onOpenSchedule: () -> Unit,
) {
    val today = LocalDate.now()
    val sessions = remember(state.courses, state.actualWeek, state.todayDayOfWeek) {
        visibleSessions(state.courses, state.actualWeek)
            .filter { it.dayOfWeek == state.todayDayOfWeek }
            .sortedBy { it.startPeriod }
    }

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }
    val nowMinutes = now.hour * 60 + now.minute

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("今日") }) },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        Text(
                            text = today.format(todayDateFormat),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(dayLabel(state.todayDayOfWeek))
                                append(" · 第 ").append(state.actualWeek).append(" 周")
                                append(" · ")
                                if (sessions.isEmpty()) {
                                    append("今天没有课")
                                } else {
                                    append("今天 ").append(sessions.size).append(" 节课")
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (sessions.isEmpty()) {
                    item {
                        EmptyTodayState(onOpenSchedule)
                    }
                } else {
                    itemsIndexed(
                        items = sessions,
                        key = { _, session -> session.sessionId },
                    ) { index, session ->
                        val startSlot = state.timeSlots
                            .firstOrNull { it.period == session.startPeriod }
                        val endPeriod = session.startPeriod + session.periodCount - 1
                        val endSlot = state.timeSlots
                            .firstOrNull { it.period == endPeriod }
                        val finished = endSlot != null && nowMinutes >= endSlot.endMinute
                        val current = startSlot != null && endSlot != null &&
                            nowMinutes >= startSlot.startMinute &&
                            nowMinutes < endSlot.endMinute
                        TodaySessionRow(
                            session = session,
                            startLabel = startSlot?.let { formatMinutes(it.startMinute) }
                                ?: periodLabel(session.startPeriod, session.periodCount),
                            endLabel = endSlot?.let { formatMinutes(it.endMinute) }
                                ?: "",
                            finished = finished,
                            current = current,
                            showTopLine = index > 0,
                            showBottomLine = index < sessions.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayState(onOpenSchedule: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("今天没有安排", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "好好休息，或者去课表里添加课程。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onOpenSchedule) {
            Text("打开课表")
        }
    }
}

@Composable
private fun TodaySessionRow(
    session: VisibleSession,
    startLabel: String,
    endLabel: String,
    finished: Boolean,
    current: Boolean,
    showTopLine: Boolean,
    showBottomLine: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = courseColors(session.colorIndex)
    val muted = finished && !current
    val timeColor = when {
        muted -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
        current -> scheme.primary
        else -> scheme.onSurface
    }
    val titleColor = when {
        muted -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val secondaryColor = if (muted) {
        scheme.onSurfaceVariant.copy(alpha = 0.55f)
    } else {
        scheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.large)
            .background(
                if (current) scheme.surfaceContainerHigh else Color.Transparent,
            )
            .semantics(mergeDescendants = true) {}
            .padding(
                start = if (current) 12.dp else 0.dp,
                end = if (current) 12.dp else 0.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .padding(top = 2.dp),
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelLarge,
                color = timeColor,
            )
            if (endLabel.isNotEmpty()) {
                Text(
                    text = endLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
        ) {
            val centerX = size.width / 2f
            val dotY = 12.dp.toPx()
            val dotRadius = 5.dp.toPx()
            val lineColor = scheme.outlineVariant.copy(alpha = 0.5f)
            if (showTopLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, dotY - dotRadius - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            if (showBottomLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, dotY + dotRadius + 2.dp.toPx()),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            drawCircle(
                color = if (muted) {
                    scheme.outline.copy(alpha = 0.6f)
                } else {
                    colors.container
                },
                radius = dotRadius,
                center = Offset(centerX, dotY),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    textDecoration = if (muted) TextDecoration.LineThrough else null,
                )
                if (current) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer,
                    ) {
                        Text(
                            text = "正在上课",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            val place = listOfNotNull(session.room, session.teacher)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (place.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = place,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${periodLabel(session.startPeriod, session.periodCount)} · " +
                    weekRangeLabel(session.startWeek, session.endWeek, session.parity),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor,
            )
        }
    }
}
