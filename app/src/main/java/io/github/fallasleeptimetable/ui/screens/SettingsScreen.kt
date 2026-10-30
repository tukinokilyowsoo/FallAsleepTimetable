package io.github.fallasleeptimetable.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.BuildConfig
import io.github.fallasleeptimetable.R
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.diagnostics.ImportLog
import io.github.fallasleeptimetable.data.TimetableEntity
import io.github.fallasleeptimetable.ui.ScheduleUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val settingsDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: ScheduleUiState,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    customColorArgb: Long?,
    onCustomColorPicked: (Long) -> Unit,
    onUpdateTerm: (TermEntity) -> Unit,
    onUpdateTimeSlots: (Int, List<Pair<Int, Int>>) -> Unit,
    onSelectTimetable: (Long) -> Unit,
    onCreateTimetable: (String) -> Unit,
    onDeleteTimetable: (Long) -> Unit,
    onRenameTimetable: (Long, String) -> Unit,
    onExportTimetable: () -> String?,
    onImportTimetable: (String) -> Unit,
    message: String?,
    onConsumeMessage: () -> Unit,
) {
    var showTermDialog by remember { mutableStateOf(false) }
    var showClassTimeDialog by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var termMenuOpen by remember { mutableStateOf(false) }
    var showCreateTimetable by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TimetableEntity?>(null) }
    var renameTarget by remember { mutableStateOf<TimetableEntity?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val json = onExportTimetable()
            if (json == null) {
                localMessage = "当前没有可导出的课表"
            } else {
                localMessage = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入文件")
                }.fold(
                    onSuccess = { "课表已导出" },
                    onFailure = { "导出失败：${it.message ?: "未知错误"}" },
                )
            }
        }
    }

    val logLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val content = buildString {
                append("app=FallAsleep ")
                append(BuildConfig.VERSION_NAME)
                append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                append("android=").append(Build.VERSION.RELEASE)
                append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
                append("device=").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                append("\n\n")
                append(ImportLog.read(context))
            }
            localMessage = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.fold(
                onSuccess = { "日志已导出" },
                onFailure = { "导出失败：${it.message ?: "未知错误"}" },
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取文件")
            }.getOrElse { error ->
                localMessage = "读取失败：${error.message ?: "未知错误"}"
                null
            }
            if (text != null) {
                onImportTimetable(text)
            }
        }
    }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("设置") }) },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item { SectionHeader("外观") }
            item {
                ListItem(
                    headlineContent = { Text("动态取色") },
                    supportingContent = { Text("跟随系统壁纸（Android 12+）") },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_palette),
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = onDynamicColorChange,
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("自定义主题色") },
                    supportingContent = {
                        Text(
                            if (dynamicColor) {
                                "选择后会自动关闭动态取色"
                            } else {
                                "当前主题色"
                            },
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_palette),
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(customColorArgb ?: DEFAULT_THEME_COLOR))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                ),
                        )
                    },
                    modifier = Modifier.clickable { showThemeColorDialog = true },
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("学期") }
            item {
                val term = state.term
                ListItem(
                    headlineContent = { Text(term?.name ?: "尚未设置") },
                    supportingContent = {
                        if (term == null) {
                            Text("创建后即可配置")
                        } else {
                            val start = LocalDate.ofEpochDay(term.startDateEpochDay)
                            Text(
                                "开学 ${start.format(settingsDateFormat)} · 共 ${term.totalWeeks} 周 · " +
                                    "每天 ${term.periodsPerDay} 节",
                            )
                        }
                    },
                    leadingContent = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    trailingContent = {
                        if (term != null) {
                            Box {
                                TextButton(onClick = { termMenuOpen = true }) { Text("设置") }
                                DropdownMenu(
                                    expanded = termMenuOpen,
                                    onDismissRequest = { termMenuOpen = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("编辑学期信息") },
                                        onClick = {
                                            termMenuOpen = false
                                            showTermDialog = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("修改上课时间") },
                                        onClick = {
                                            termMenuOpen = false
                                            showClassTimeDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("课表") }
            state.timetables.forEach { timetable ->
                item(key = "timetable-${timetable.id}") {
                    ListItem(
                        headlineContent = { Text(timetable.name) },
                        leadingContent = {
                            RadioButton(
                                selected = state.selectedTimetable?.id == timetable.id,
                                onClick = { onSelectTimetable(timetable.id) },
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { renameTarget = timetable }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "重命名课表",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (state.timetables.size > 1) {
                                    IconButton(onClick = { deleteTarget = timetable }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "删除课表",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable { onSelectTimetable(timetable.id) },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("新建课表") },
                    leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                    modifier = Modifier.clickable { showCreateTimetable = true },
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("数据") }
            item {
                ListItem(
                    headlineContent = { Text("导出当前课表") },
                    supportingContent = { Text("保存为 FallAsleep JSON 备份") },
                    leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        exportLauncher.launch("FallAsleep课表.json")
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("导入课表") },
                    supportingContent = { Text("支持 JSON 备份与旧版 .wakeup_schedule") },
                    leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("*/*"))
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("导出日志") },
                    supportingContent = { Text("反馈教务解析问题用，可能包含课表页内容") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.clickable {
                        logLauncher.launch("FallAsleep-log.txt")
                    },
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("关于") }
            item {
                ListItem(
                    headlineContent = { Text("FallAsleep 课程表") },
                    supportingContent = { Text("版本 ${BuildConfig.VERSION_NAME} · M1") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                )
            }
            item { Spacer(modifier = Modifier.width(1.dp).padding(bottom = 16.dp)) }
        }
    }

    if (showTermDialog && state.term != null) {
        TermSettingsDialog(
            term = state.term!!,
            onDismiss = { showTermDialog = false },
            onSave = { updated ->
                onUpdateTerm(updated)
                showTermDialog = false
            },
        )
    }

    if (showClassTimeDialog) {
        val term = state.term
        if (term != null) {
            ClassTimeSettingsDialog(
                term = term,
                slots = state.timeSlots,
                onDismiss = { showClassTimeDialog = false },
                onSave = { periods, slots ->
                    onUpdateTimeSlots(periods, slots)
                    showClassTimeDialog = false
                },
            )
        }
    }

    if (showCreateTimetable) {
        CreateTimetableDialog(
            onDismiss = { showCreateTimetable = false },
            onCreate = { name ->
                onCreateTimetable(name)
                showCreateTimetable = false
            },
        )
    }

    renameTarget?.let { target ->
        var name by remember(target.id) { mutableStateOf(target.name) }

        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名课表") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课表名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = name.trim()
                        if (newName.isNotEmpty()) {
                            onRenameTimetable(target.id, newName)
                        }
                        renameTarget = null
                    },
                    enabled = name.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除课表？") },
            text = { Text("「${target.name}」里的所有课程都会被删除，无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTimetable(target.id)
                        deleteTarget = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }

    if (showThemeColorDialog) {
        ThemeColorDialog(
            initialColor = customColorArgb,
            onDismiss = { showThemeColorDialog = false },
            onApply = { argb ->
                onCustomColorPicked(argb)
                showThemeColorDialog = false
            },
        )
    }

    (localMessage ?: message)?.let { text ->
        AlertDialog(
            onDismissRequest = {
                localMessage = null
                onConsumeMessage()
            },
            title = { Text("提示") },
            text = { Text(text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        localMessage = null
                        onConsumeMessage()
                    },
                ) { Text("好") }
            },
        )
    }
}

private const val DEFAULT_THEME_COLOR = 0xFF4A5C92L

private val themeColorPresets = listOf(
    0xFF4A5C92L, 0xFF00696EL, 0xFF2E6A3EL, 0xFF8C5000L,
    0xFF9C4146L, 0xFF7D5260L, 0xFF3F5F90L, 0xFF5D5F7FL,
    0xFF6B5778L, 0xFF00658FL, 0xFF7A5900L, 0xFF4F6354L,
)

@Composable
private fun ThemeColorDialog(
    initialColor: Long?,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    var selected by remember { mutableStateOf(initialColor ?: DEFAULT_THEME_COLOR) }
    var hexText by remember {
        mutableStateOf(String.format("#%06X", selected and 0xFFFFFF))
    }
    var hexError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义主题色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                themeColorPresets.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset))
                                    .border(
                                        width = if (selected == preset) 3.dp else 1.dp,
                                        color = if (selected == preset) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        selected = preset
                                        hexText = String.format(
                                            "#%06X",
                                            preset and 0xFFFFFF,
                                        )
                                        hexError = false
                                    },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val parsed = parseHexColor(input)
                        if (parsed != null) {
                            selected = parsed
                            hexError = false
                        } else {
                            hexError = true
                        }
                    },
                    label = { Text("十六进制颜色") },
                    singleLine = true,
                    isError = hexError,
                    supportingText = {
                        if (hexError) Text("格式应为 #RRGGBB")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("预览", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(selected)),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(selected) },
                enabled = !hexError,
            ) { Text("应用") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun parseHexColor(input: String): Long? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return 0xFF000000L or value
}

@Composable
private fun SectionHeader(title: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
