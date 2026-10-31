package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.data.share.ScheduleImageExporter
import io.github.fallasleeptimetable.data.share.SharePalette
import io.github.fallasleeptimetable.ui.theme.colorSchemeFromSeed

private enum class ShareTheme(val label: String) {
    CURRENT("当前主题"),
    LIGHT("浅色"),
    DARK("深色"),
}

private enum class ShareRange(val label: String) {
    CURRENT("当前周"),
    ALL("整学期"),
}

@Composable
fun ShareImageDialog(
    onDismiss: () -> Unit,
    onShare: (ScheduleImageExporter.Options) -> Unit,
) {
    var theme by remember { mutableStateOf(ShareTheme.CURRENT) }
    var range by remember { mutableStateOf(ShareRange.CURRENT) }
    var includeTeacher by remember { mutableStateOf(true) }
    var includeRoom by remember { mutableStateOf(true) }

    val scheme = MaterialTheme.colorScheme
    val palettes = remember(scheme) {
        mapOf(
            ShareTheme.CURRENT to SharePalette.from(scheme),
            ShareTheme.LIGHT to SharePalette.from(
                colorSchemeFromSeed(scheme.primary, dark = false),
            ),
            ShareTheme.DARK to SharePalette.from(
                colorSchemeFromSeed(scheme.primary, dark = true),
            ),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享课表长图") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("主题", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareTheme.entries.forEach { option ->
                        FilterChip(
                            selected = theme == option,
                            onClick = { theme = option },
                            label = { Text(option.label) },
                        )
                    }
                }

                Text("范围", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareRange.entries.forEach { option ->
                        FilterChip(
                            selected = range == option,
                            onClick = { range = option },
                            label = { Text(option.label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("显示教师", modifier = Modifier.weight(1f))
                    Switch(
                        checked = includeTeacher,
                        onCheckedChange = { includeTeacher = it },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("显示教室", modifier = Modifier.weight(1f))
                    Switch(
                        checked = includeRoom,
                        onCheckedChange = { includeRoom = it },
                    )
                }

                Text(
                    text = if (range == ShareRange.ALL) {
                        "整学期长图较高，生成需要几秒；完成后面板可直接保存或发送。"
                    } else {
                        "按当前显示的第几周生成一张图片，完成后面板可直接保存或发送。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onShare(
                        ScheduleImageExporter.Options(
                            includeTeacher = includeTeacher,
                            includeRoom = includeRoom,
                            allWeeks = range == ShareRange.ALL,
                            palette = palettes.getValue(theme),
                        ),
                    )
                },
            ) { Text("生成并分享") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
