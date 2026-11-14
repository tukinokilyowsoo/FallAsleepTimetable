package io.github.fallasleeptimetable.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val termDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermSettingsDialog(
    term: TermEntity,
    onDismiss: () -> Unit,
    onSave: (TermEntity) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(term.name) }
    var startDate by remember { mutableStateOf(LocalDate.ofEpochDay(term.startDateEpochDay)) }
    var totalWeeks by rememberSaveable { mutableStateOf(term.totalWeeks) }
    var showSaturday by rememberSaveable { mutableStateOf(term.showSaturday) }
    var showSunday by rememberSaveable { mutableStateOf(term.showSunday) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                    Text("学期设置", style = MaterialTheme.typography.headlineSmall)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("学期名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("开学日期", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = startDate.format(termDateFormat),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { showDatePicker = true }) { Text("选择") }
                    }

                    HorizontalDivider()

                    TermStepper(
                        label = "总周数",
                        value = totalWeeks,
                        min = 8,
                        max = 30,
                        suffix = " 周",
                        onChange = { totalWeeks = it },
                    )

                    HorizontalDivider()

                    SwitchRow(
                        label = "显示周六",
                        checked = showSaturday,
                        onCheckedChange = { showSaturday = it },
                    )
                    SwitchRow(
                        label = "显示周日",
                        checked = showSunday,
                        onCheckedChange = { showSunday = it },
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSave(
                                    term.copy(
                                        name = name.trim().ifBlank { term.name },
                                        startDateEpochDay = startDate.toEpochDay(),
                                        totalWeeks = totalWeeks,
                                        showSaturday = showSaturday,
                                        showSunday = showSunday,
                                    ),
                                )
                            },
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TermStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    suffix: String,
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
            text = "$value$suffix",
            modifier = Modifier.widthIn(min = 56.dp),
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

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
