package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.domain.CourseDraft
import io.github.fallasleeptimetable.domain.VisibleSession
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.periodLabel
import io.github.fallasleeptimetable.ui.weekRangeLabel

data class EditorRequest(
    val courseId: Long? = null,
    val sessionId: Long? = null,
    val name: String = "",
    val teacher: String = "",
    val room: String = "",
    val dayOfWeek: Int = 1,
    val startPeriod: Int = 1,
    val periodCount: Int = 2,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val parity: Int = 0,
    val colorIndex: Int = 0,
)

@Composable
fun CourseDetailDialog(
    session: VisibleSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(session.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${dayLabel(session.dayOfWeek)} · ${periodLabel(session.startPeriod, session.periodCount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DetailLine("周次", weekRangeLabel(session.startWeek, session.endWeek, session.parity))
                DetailLine("地点", session.room ?: "未填写")
                DetailLine("教师", session.teacher ?: "未填写")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Button(onClick = onEdit) { Text("编辑") }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除课程？") },
            text = { Text("将删除「${session.name}」的所有上课安排。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorDialog(
    request: EditorRequest,
    term: TermEntity,
    onDismiss: () -> Unit,
    onSave: (CourseDraft) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(request.name) }
    var teacher by rememberSaveable { mutableStateOf(request.teacher) }
    var room by rememberSaveable { mutableStateOf(request.room) }
    var day by rememberSaveable { mutableStateOf(request.dayOfWeek) }
    var startPeriod by rememberSaveable { mutableStateOf(request.startPeriod) }
    var periodCount by rememberSaveable { mutableStateOf(request.periodCount) }
    var startWeek by rememberSaveable { mutableStateOf(request.startWeek) }
    var endWeek by rememberSaveable { mutableStateOf(request.endWeek) }
    var parity by rememberSaveable { mutableStateOf(request.parity) }
    var colorIndex by rememberSaveable { mutableStateOf(request.colorIndex) }

    val maxPeriods = term.periodsPerDay
    val totalWeeks = term.totalWeeks

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 720.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (request.courseId == null) "添加课程" else "编辑课程",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("课程名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("教师（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("教室（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    SectionLabel("星期")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (1..7).forEach { dayValue ->
                            FilterChip(
                                selected = day == dayValue,
                                onClick = { day = dayValue },
                                label = { Text(dayLabel(dayValue)) },
                            )
                        }
                    }

                    SectionLabel("节次")
                    StepperRow(
                        label = "开始节次",
                        value = startPeriod,
                        min = 1,
                        max = maxPeriods,
                        onChange = {
                            startPeriod = it
                            periodCount = periodCount.coerceAtMost(maxPeriods - startPeriod + 1)
                        },
                    )
                    StepperRow(
                        label = "连续节数",
                        value = periodCount,
                        min = 1,
                        max = maxPeriods - startPeriod + 1,
                        onChange = { periodCount = it },
                    )

                    SectionLabel("周次")
                    StepperRow(
                        label = "开始周",
                        value = startWeek,
                        min = 1,
                        max = totalWeeks,
                        onChange = {
                            startWeek = it
                            if (endWeek < startWeek) endWeek = startWeek
                        },
                    )
                    StepperRow(
                        label = "结束周",
                        value = endWeek,
                        min = startWeek,
                        max = totalWeeks,
                        onChange = { endWeek = it },
                    )

                    SectionLabel("单双周")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "全部", 1 to "单周", 2 to "双周").forEach { (value, label) ->
                            FilterChip(
                                selected = parity == value,
                                onClick = { parity = value },
                                label = { Text(label) },
                            )
                        }
                    }

                    SectionLabel("颜色")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(6) { index ->
                            val colors = courseColors(index)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.container)
                                    .border(
                                        width = if (colorIndex == index) 3.dp else 1.dp,
                                        color = if (colorIndex == index) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape,
                                    )
                                    .clickable { colorIndex = index },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val safeStartPeriod = startPeriod.coerceIn(1, maxPeriods)
                                val safeCount = periodCount.coerceIn(
                                    1,
                                    maxPeriods - safeStartPeriod + 1,
                                )
                                val safeStartWeek = startWeek.coerceIn(1, totalWeeks)
                                val safeEndWeek = endWeek.coerceIn(safeStartWeek, totalWeeks)
                                onSave(
                                    CourseDraft(
                                        name = name.trim(),
                                        teacher = teacher,
                                        room = room,
                                        dayOfWeek = day,
                                        startPeriod = safeStartPeriod,
                                        periodCount = safeCount,
                                        startWeek = safeStartWeek,
                                        endWeek = safeEndWeek,
                                        parity = parity,
                                        colorIndex = colorIndex,
                                    ),
                                )
                            },
                            enabled = name.isNotBlank(),
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString(),
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
