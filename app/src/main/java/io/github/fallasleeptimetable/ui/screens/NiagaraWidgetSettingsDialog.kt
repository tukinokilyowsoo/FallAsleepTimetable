package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.widget.NiagaraWidgetSetting
import kotlin.math.roundToInt

private val fontPresets = listOf(0, 12, 14, 16, 18, 20, 24, 28, 32)

/**
 * 一个入口集中设置 Niagara 透明小组件：行数、字号、文字颜色。
 */
@Composable
fun NiagaraWidgetSettingsDialog(
    initialRows: Int,
    initialFontSize: Int,
    initialTextColor: String,
    onDismiss: () -> Unit,
    onApply: (rows: Int, fontSize: Int, textColor: String) -> Unit,
) {
    var rows by remember { mutableIntStateOf(initialRows.coerceIn(1, 8)) }
    var fontIndex by remember {
        mutableIntStateOf(
            fontPresets.indexOf(initialFontSize).takeIf { it >= 0 } ?: 0,
        )
    }
    var textColor by remember { mutableStateOf(initialTextColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Niagara 小组件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "显示课程数：$rows 节",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = rows.toFloat(),
                    onValueChange = { rows = it.roundToInt().coerceIn(1, 8) },
                    valueRange = 1f..12f,
                    steps = 10,
                )

                Text(
                    text = "课程名字号：${NiagaraWidgetSetting.fontSizeLabel(fontPresets[fontIndex])}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = fontIndex.toFloat(),
                    onValueChange = {
                        fontIndex = it.roundToInt()
                            .coerceIn(0, fontPresets.lastIndex)
                    },
                    valueRange = 0f..fontPresets.lastIndex.toFloat(),
                    steps = (fontPresets.size - 2).coerceAtLeast(0),
                )

                Text(
                    text = "文字颜色",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        NiagaraWidgetSetting.TEXT_AUTO to "自动",
                        NiagaraWidgetSetting.TEXT_BLACK to "黑色",
                        NiagaraWidgetSetting.TEXT_WHITE to "白色",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = textColor == value,
                            onClick = { textColor = value },
                            label = { Text(label) },
                        )
                    }
                }

                Text(
                    text = "不足设定数量时用空行占位，避免下课后字号忽大忽小；" +
                        "字号自动适配并留安全余量，手动只能调小。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(rows, fontPresets[fontIndex], textColor)
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
