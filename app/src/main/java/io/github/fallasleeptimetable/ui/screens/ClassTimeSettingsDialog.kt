package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.ui.formatMinutes

private data class TimePickRequest(
    val initialMinutes: Int,
    val onPicked: (Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassTimeSettingsDialog(
    term: TermEntity,
    slots: List<TimeSlotEntity>,
    onDismiss: () -> Unit,
    onSave: (periodsPerDay: Int, slots: List<Pair<Int, Int>>) -> Unit,
) {
    val initialTimes = remember {
        val fromDb = slots.sortedBy { it.period }.map { it.startMinute to it.endMinute }
        if (fromDb.isNotEmpty()) fromDb else defaultTimes()
    }
    val initialMorningCount = remember {
        initialTimes.count { it.first < 12 * 60 }.coerceIn(1, initialTimes.size)
    }
    val initialAfternoonCount = (initialTimes.size - initialMorningCount).coerceAtLeast(0)
    val initialDuration = remember {
        val first = initialTimes.firstOrNull()
        ((first?.second ?: 0) - (first?.first ?: 0)).takeIf { it in 20..120 } ?: 45
    }
    val initialBreak = remember {
        initialTimes.zipWithNext { a, b -> b.first - a.second }
            .firstOrNull { it in 0..30 } ?: 10
    }

    var step by rememberSaveable { mutableStateOf(0) }
    var morningCount by rememberSaveable { mutableStateOf(initialMorningCount) }
    var afternoonCount by rememberSaveable { mutableStateOf(initialAfternoonCount) }
    var duration by rememberSaveable { mutableStateOf(initialDuration) }
    var breakMinutes by rememberSaveable { mutableStateOf(initialBreak) }
    var morningStart by rememberSaveable {
        mutableStateOf(initialTimes.firstOrNull()?.first ?: (8 * 60))
    }
    var afternoonStart by rememberSaveable {
        mutableStateOf(initialTimes.getOrNull(initialMorningCount)?.first ?: (14 * 60))
    }

    val times = remember {
        mutableStateListOf<Pair<Int, Int>>().apply { addAll(initialTimes) }
    }

    var needsRegenerate by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pickerRequest by remember { mutableStateOf<TimePickRequest?>(null) }

    fun markChanged() {
        needsRegenerate = true
        errorText = null
    }

    fun buildTimes(): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()

        var current = morningStart
        repeat(morningCount) {
            val end = current + duration
            result.add(current to end)
            current = end + breakMinutes
        }

        current = afternoonStart
        repeat(afternoonCount) {
            val end = current + duration
            result.add(current to end)
            current = end + breakMinutes
        }
        return result
    }

    fun chainAfter(index: Int) {
        val startIndex = index + 1
        if (startIndex >= times.size) return
        // 按“被修改的这一节”所在半天判断边界；上午最后一节改完后不能顺延到下午。
        val blockEnd = if (index < morningCount) morningCount else times.size
        if (startIndex >= blockEnd) return

        var current = times[index].second + breakMinutes
        for (i in startIndex until blockEnd) {
            val end = current + duration
            times[i] = current to end
            current = end + breakMinutes
        }
    }

    fun setPeriodStart(index: Int, minutes: Int) {
        val safe = minutes.coerceIn(0, 24 * 60 - duration)
        times[index] = safe to (safe + duration)
        chainAfter(index)
        errorText = null
    }

    fun setPeriodEnd(index: Int, minutes: Int) {
        val newStart = minutes - duration
        if (newStart < 0) {
            errorText = "结束时间不能早于一节课的时长"
            return
        }
        times[index] = newStart to minutes
        chainAfter(index)
        errorText = null
    }

    fun pick(initial: Int, onPicked: (Int) -> Unit) {
        pickerRequest = TimePickRequest(initial.coerceIn(0, 24 * 60 - 1), onPicked)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 820.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("修改上课时间", style = MaterialTheme.typography.headlineSmall)

                    if (step == 0) {
                        Text(
                            text = "第 1 步 · 总体设置",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        ClassTimeStepper(
                            label = "上午课程数",
                            value = morningCount,
                            min = 1,
                            max = 12,
                            step = 1,
                            suffix = " 节",
                            onChange = {
                                morningCount = it
                                markChanged()
                            },
                        )
                        ClassTimeStepper(
                            label = "下午课程数",
                            value = afternoonCount,
                            min = 0,
                            max = 12,
                            step = 1,
                            suffix = " 节",
                            onChange = {
                                afternoonCount = it
                                markChanged()
                            },
                        )
                        ClassTimeStepper(
                            label = "一节课时长",
                            value = duration,
                            min = 20,
                            max = 120,
                            step = 5,
                            suffix = " 分钟",
                            onChange = {
                                duration = it.coerceIn(20, 120)
                                markChanged()
                            },
                        )
                        ClassTimeStepper(
                            label = "课间休息",
                            value = breakMinutes,
                            min = 0,
                            max = 30,
                            step = 5,
                            suffix = " 分钟",
                            onChange = {
                                breakMinutes = it.coerceIn(0, 30)
                                markChanged()
                            },
                        )

                        TimeRow(
                            label = "上午第一节课开始",
                            minutes = morningStart,
                            onPick = {
                                pick(morningStart) { picked ->
                                    morningStart = picked
                                    markChanged()
                                }
                            },
                        )
                        if (afternoonCount > 0) {
                            TimeRow(
                                label = "下午第一节课开始",
                                minutes = afternoonStart,
                                onPick = {
                                    pick(afternoonStart) { picked ->
                                        afternoonStart = picked
                                        markChanged()
                                    }
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onDismiss) { Text("取消") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (
                                        needsRegenerate ||
                                        times.size != morningCount + afternoonCount
                                    ) {
                                        times.clear()
                                        times.addAll(buildTimes())
                                        needsRegenerate = false
                                    }
                                    errorText = null
                                    step = 1
                                },
                            ) {
                                Text("下一步")
                            }
                        }
                    } else {
                        Text(
                            text = "第 2 步 · 每节课时间",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "上午 $morningCount 节 · 下午 $afternoonCount 节 · " +
                                "每节 $duration 分钟 · 课间 $breakMinutes 分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        HorizontalDivider()
                        Text(
                            text = "改开始时间会自动补结束时间；改结束时间会自动回推开始时间，同段后续节次按课间顺延。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        times.forEachIndexed { index, pair ->
                            if (index == 0) {
                                Text(
                                    text = "上午",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (index == morningCount) {
                                Text(
                                    text = "下午",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "第 ${index + 1} 节",
                                    modifier = Modifier.width(72.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                TextButton(
                                    onClick = {
                                        val start = times[index].first
                                        pick(start) { picked -> setPeriodStart(index, picked) }
                                    },
                                ) {
                                    Text(formatMinutes(pair.first))
                                }
                                Text("—", style = MaterialTheme.typography.bodySmall)
                                TextButton(
                                    onClick = {
                                        val end = times[index].second
                                        pick(end) { picked -> setPeriodEnd(index, picked) }
                                    },
                                ) {
                                    Text(formatMinutes(pair.second))
                                }
                            }
                        }

                        errorText?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { step = 0 }) { Text("上一步") }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val total = morningCount + afternoonCount
                                    val invalid = times.size != total ||
                                        times.any {
                                            it.second <= it.first || it.second > 24 * 60
                                        }
                                    if (invalid) {
                                        errorText = "时间设置有误，请检查每节的起止时间"
                                    } else {
                                        onSave(total, times.toList())
                                    }
                                },
                            ) {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }

    pickerRequest?.let { request ->
        Material3TimePickerDialog(
            initialMinutes = request.initialMinutes,
            onDismiss = { pickerRequest = null },
            onConfirm = { picked ->
                pickerRequest = null
                request.onPicked(picked)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Material3TimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("选择时间", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val layoutType = if (maxWidth >= 480.dp) {
                            TimePickerLayoutType.Horizontal
                        } else {
                            TimePickerLayoutType.Vertical
                        }
                        TimePicker(
                            state = state,
                            layoutType = layoutType,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(state.hour * 60 + state.minute) },
                        ) { Text("确定") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    minutes: Int,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onPick) { Text(formatMinutes(minutes)) }
    }
}

@Composable
private fun ClassTimeStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(
            onClick = { onChange((value - step).coerceAtLeast(min)) },
            enabled = value > min,
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "$value$suffix",
            modifier = Modifier.widthIn(min = 88.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(
            onClick = { onChange((value + step).coerceAtMost(max)) },
            enabled = value < max,
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun defaultTimes(): List<Pair<Int, Int>> {
    var current = 8 * 60
    return buildList {
        repeat(12) {
            val end = current + 45
            add(current to end)
            current = end + 10
        }
    }
}
