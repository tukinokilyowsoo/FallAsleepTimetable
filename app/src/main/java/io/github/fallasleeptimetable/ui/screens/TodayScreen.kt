package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.ui.ScheduleUiState
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.formatMinutes
import io.github.fallasleeptimetable.ui.periodLabel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val todayDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(state: ScheduleUiState) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "${today.format(todayDateFormat)} · ${dayLabel(state.todayDayOfWeek)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (sessions.isEmpty()) {
                            "今天没有课"
                        } else {
                            "第 ${state.actualWeek} 周 · 今天有 ${sessions.size} 节课"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("今天没有安排", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "好好休息，或者去课表里添加课程。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(sessions, key = { it.sessionId }) { session ->
                    val startSlot = state.timeSlots.firstOrNull { it.period == session.startPeriod }
                    val endPeriod = session.startPeriod + session.periodCount - 1
                    val endSlot = state.timeSlots.firstOrNull { it.period == endPeriod }
                    val finished = endSlot != null && nowMinutes >= endSlot.endMinute
                    val timeText = if (startSlot != null && endSlot != null) {
                        "${formatMinutes(startSlot.startMinute)} – ${formatMinutes(endSlot.endMinute)}"
                    } else {
                        periodLabel(session.startPeriod, session.periodCount)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = timeText,
                                modifier = Modifier.width(88.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (finished) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (finished) {
                                                    MaterialTheme.colorScheme.outline
                                                } else {
                                                    courseColors(session.colorIndex).container
                                                },
                                            ),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (finished) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        textDecoration = if (finished) {
                                            TextDecoration.LineThrough
                                        } else {
                                            null
                                        },
                                    )
                                }
                                Text(
                                    text = listOfNotNull(
                                        session.room,
                                        session.teacher,
                                    ).joinToString(" · ").ifBlank {
                                        periodLabel(session.startPeriod, session.periodCount)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (finished) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
