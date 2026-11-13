package io.github.fallasleeptimetable.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import io.github.fallasleeptimetable.data.importer.ImportSettingsSummary
import io.github.fallasleeptimetable.data.importer.TimetableImporter
import io.github.fallasleeptimetable.data.share.ScheduleImageExporter
import io.github.fallasleeptimetable.data.share.ShareLauncher
import io.github.fallasleeptimetable.ui.ScheduleUiState
import io.github.fallasleeptimetable.ui.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 课表页右上角的「分享与导出」：长图、JSON、CSV、文件导入集中在这里。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareExportSheet(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    val sheetIsLight =
        MaterialTheme.colorScheme.surfaceContainerLow.luminance() > 0.5f

    // 分享弹窗位于独立的 Dialog 窗口，系统默认会给手势条加对比度底色；
    // 这里关掉对比度并让导航栏透明，使弹窗背景一直铺到屏幕底部。
    DisposableEffect(view, sheetIsLight) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightNavigationBars = sheetIsLight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose { }
    }
    var showShareImage by remember { mutableStateOf(false) }
    var shareBusy by remember { mutableStateOf(false) }
    var pendingImport by remember {
        mutableStateOf<Pair<String, ImportSettingsSummary>?>(null)
    }

    fun saveText(uri: Uri?, content: String?, successMessage: String) {
        if (uri == null) return
        if (content == null) {
            onMessage("当前课表没有可导出的课程")
            onDismiss()
            return
        }
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入文件")
        }
        result.fold(
            onSuccess = { onMessage(successMessage) },
            onFailure = { onMessage("导出失败：${it.message ?: "未知错误"}") },
        )
        onDismiss()
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> saveText(uri, viewModel.exportTimetableJson(), "课表已导出为 JSON") }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> saveText(uri, viewModel.exportTimetableCsv(), "课表已导出为 CSV") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("无法读取文件")
        }.getOrElse { error ->
            onMessage("读取失败：${error.message ?: "未知错误"}")
            null
        }
        if (text != null) {
            val summary = TimetableImporter.inspect(text)
            if (summary != null) {
                pendingImport = text to summary
            } else {
                viewModel.importTimetableText(text) { onMessage(it) }
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = "分享与导出",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
            Text(
                text = "课程表长图、备份与文件交换",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))

            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("分享课表长图") },
                supportingContent = {
                    Text(
                        if (shareBusy) {
                            "正在生成图片…"
                        } else {
                            "本周或整学期 PNG，可保存或发送"
                        },
                    )
                },
                leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !shareBusy) {
                    showShareImage = true
                },
            )
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("导出 JSON 备份") },
                supportingContent = { Text("完整备份，可在本应用恢复") },
                leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                modifier = Modifier.clickable {
                    exportJsonLauncher.launch("FallAsleep课表.json")
                },
            )
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("导出 CSV 表格") },
                supportingContent = { Text("可导入 Excel 或其他课程表工具") },
                leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                modifier = Modifier.clickable {
                    exportCsvLauncher.launch("FallAsleep课表.csv")
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("导入课表文件") },
                supportingContent = {
                    Text("JSON / ICS / CSV / 旧版 .wakeup_schedule")
                },
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("*/*"))
                },
            )
        }
    }

    if (showShareImage) {
        ShareImageDialog(
            onDismiss = { showShareImage = false },
            onShare = { options ->
                showShareImage = false
                val term = state.term
                val table = state.selectedTimetable
                if (term == null || table == null) {
                    onMessage("还没有可分享的课表")
                } else {
                shareBusy = true
                scope.launch {
                    val outcome = withContext(Dispatchers.Default) {
                        runCatching {
                            ScheduleImageExporter.renderPng(
                                term = term,
                                timetableName = table.name,
                                courses = state.courses,
                                timeSlots = state.timeSlots,
                                currentWeek = state.currentWeek,
                                options = options,
                            )
                        }
                    }
                    shareBusy = false
                    outcome.fold(
                        onSuccess = { png ->
                            if (png == null) {
                                onMessage("当前课表还没有课程")
                            } else {
                                runCatching {
                                    ShareLauncher.sharePng(
                                        context,
                                        png,
                                        "FallAsleep课表.png",
                                    )
                                }.onFailure {
                                    onMessage("分享失败：${it.message ?: "未知错误"}")
                                }
                            }
                        },
                        onFailure = {
                            onMessage("生成图片失败：${it.message ?: "未知错误"}")
                        },
                    )
                    onDismiss()
                }
                }
            },
        )
    }

    pendingImport?.let { (text, summary) ->
        val details = buildList {
            summary.startDateEpochDay?.let {
                add("开学日期 ${java.time.LocalDate.ofEpochDay(it)}")
            }
            summary.totalWeeks?.let { add("共 $it 周") }
            summary.periodsPerDay?.let { add("每天 $it 节") }
            if (summary.timeSlotCount > 0) {
                add("节次时间 ${summary.timeSlotCount} 条")
            }
        }
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("一并恢复学期设置？") },
            text = {
                Text(
                    "这份 JSON 还包含：\n" + details.joinToString("\n") +
                        "\n\n恢复会覆盖当前学期（以及该学期下所有课表）共用的" +
                        "开学日期、周数和上课时间。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        viewModel.importTimetableText(text, true) { onMessage(it) }
                        onDismiss()
                    },
                ) { Text("一并恢复") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingImport = null }) { Text("取消") }
                    TextButton(
                        onClick = {
                            pendingImport = null
                            viewModel.importTimetableText(text, false) { onMessage(it) }
                            onDismiss()
                        },
                    ) { Text("仅导入课程") }
                }
            },
        )
    }
}
