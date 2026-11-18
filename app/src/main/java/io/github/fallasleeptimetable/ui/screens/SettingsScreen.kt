package io.github.fallasleeptimetable.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.fallasleeptimetable.BuildConfig
import io.github.fallasleeptimetable.R
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.data.calendar.CalendarScheduleExporter
import io.github.fallasleeptimetable.data.diagnostics.ImportLog
import io.github.fallasleeptimetable.data.TimetableEntity
import io.github.fallasleeptimetable.ui.ScheduleUiState
import io.github.fallasleeptimetable.ui.theme.DEFAULT_THEME_SEED
import io.github.fallasleeptimetable.ui.theme.ThemeMode
import io.github.fallasleeptimetable.ui.theme.androidThemePalettes
import io.github.fallasleeptimetable.ui.theme.colorSchemeFromSeed
import io.github.fallasleeptimetable.widget.WidgetRefreshScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val settingsDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: ScheduleUiState,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    customColorArgb: Long?,
    onCustomColorPicked: (Long) -> Unit,
    darkMode: String,
    onDarkModeChange: (String) -> Unit,
    onShowWelcome: () -> Unit,
    onUpdateTerm: (TermEntity) -> Unit,
    onUpdateTimeSlots: (Long, Int, List<Pair<Int, Int>>, Boolean) -> Unit,
    onLoadTimeSlots: suspend (Long) -> List<TimeSlotEntity>,
    onSelectTimetable: (Long) -> Unit,
    onCreateTimetable: (String) -> Unit,
    onDeleteTimetable: (Long) -> Unit,
    onRenameTimetable: (Long, String) -> Unit,
    onExportIcs: () -> String?,
    message: String?,
    onConsumeMessage: () -> Unit,
) {
    var showTermDialog by remember { mutableStateOf(false) }
    var showTimeTargetPicker by remember { mutableStateOf(false) }
    var showClassTimeDialog by remember { mutableStateOf(false) }
    var timeTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var timeTargetSlots by remember { mutableStateOf<List<TimeSlotEntity>>(emptyList()) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var darkModeMenuOpen by remember { mutableStateOf(false) }
    var termMenuOpen by remember { mutableStateOf(false) }
    var showCreateTimetable by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TimetableEntity?>(null) }
    var renameTarget by remember { mutableStateOf<TimetableEntity?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var calendarBusy by remember { mutableStateOf(false) }
    var pendingCalendarAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var deleteCurrentCalendarConfirm by remember { mutableStateOf(false) }
    var deleteAllCalendarConfirm by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showThirdPartyNotices by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showContactDeveloper by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showEduUrlDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsPrefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var eduUrl by remember {
        mutableStateOf(settingsPrefs.getString("edu_url", "").orEmpty())
    }
    var updateReminders by remember {
        mutableStateOf(settingsPrefs.getBoolean("update_reminders", true))
    }

    fun runCalendarExport() {
        if (calendarBusy) return
        val term = state.term
        val timetable = state.selectedTimetable
        if (term == null || timetable == null) {
            localMessage = "还没有可导入的课表"
            return
        }
        if (state.courses.isEmpty()) {
            localMessage = "当前课表还没有课程"
            return
        }

        calendarBusy = true
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    CalendarScheduleExporter.export(
                        context = context,
                        term = term,
                        timetableId = timetable.id,
                        courses = state.courses,
                        timeSlots = state.timeSlots,
                    )
                }
            }
            calendarBusy = false
            localMessage = outcome.fold(
                onSuccess = { result ->
                    if (result.eventCount == 0) {
                        "没有可导入的日程"
                    } else {
                        "已导入 ${result.eventCount} 个日程，可在系统日历中启用「${result.calendarDisplayName}」"
                    }
                },
                onFailure = { error ->
                    "导入失败：${error.message ?: "未知错误"}"
                },
            )
        }
    }

    fun runDeleteCurrentCalendar() {
        if (calendarBusy) return
        val timetable = state.selectedTimetable
        if (timetable == null) {
            localMessage = "还没有选择课表"
            return
        }
        calendarBusy = true
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    CalendarScheduleExporter.deleteForTimetable(context, timetable.id)
                }
            }
            calendarBusy = false
            localMessage = outcome.fold(
                onSuccess = { count ->
                    if (count == 0) {
                        "没有找到当前课表已导入的日程"
                    } else {
                        "已删除 $count 个当前课表日程"
                    }
                },
                onFailure = { "删除失败：${it.message ?: "未知错误"}" },
            )
        }
    }

    fun runDeleteAllCalendar() {
        if (calendarBusy) return
        calendarBusy = true
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { CalendarScheduleExporter.deleteAll(context) }
            }
            calendarBusy = false
            localMessage = outcome.fold(
                onSuccess = { count ->
                    if (count == 0) {
                        "没有找到已导入的日程"
                    } else {
                        "已删除 $count 个已导入日程"
                    }
                },
                onFailure = { "删除失败：${it.message ?: "未知错误"}" },
            )
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val action = pendingCalendarAction
        pendingCalendarAction = null
        if (action != null) {
            if (grants.values.all { it }) {
                action()
            } else {
                localMessage = "没有日历权限，操作已取消"
            }
        }
    }

    fun withCalendarPermission(action: () -> Unit) {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )
        val granted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            action()
        } else {
            pendingCalendarAction = action
            calendarPermissionLauncher.launch(permissions)
        }
    }

    fun saveText(uri: Uri?, content: String?, successMessage: String) {
        if (uri == null) return
        if (content == null) {
            localMessage = "当前课表没有可导出的课程"
            return
        }
        localMessage = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入文件")
        }.fold(
            onSuccess = { successMessage },
            onFailure = { "导出失败：${it.message ?: "未知错误"}" },
        )
    }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri -> saveText(uri, onExportIcs(), "课表已导出为 ICS") }

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

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("设置") }) },
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
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
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
                    headlineContent = { Text("深色模式") },
                    supportingContent = { Text(ThemeMode.label(darkMode)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_palette),
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { darkModeMenuOpen = true }) {
                                Text("设置")
                            }
                            DropdownMenu(
                                expanded = darkModeMenuOpen,
                                onDismissRequest = { darkModeMenuOpen = false },
                            ) {
                                listOf(
                                    ThemeMode.SYSTEM to "跟随系统",
                                    ThemeMode.LIGHT to "浅色",
                                    ThemeMode.DARK to "深色",
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            darkModeMenuOpen = false
                                            onDarkModeChange(value)
                                        },
                                    )
                                }
                            }
                        }
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
                                .background(Color(customColorArgb ?: DEFAULT_THEME_SEED))
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

            item { SectionHeader("小组件") }
            item {
                val exactAllowed = WidgetRefreshScheduler.canScheduleExact(context)
                ListItem(
                    headlineContent = { Text("下课准时刷新") },
                    supportingContent = {
                        Text(
                            if (exactAllowed) {
                                "已允许精确闹钟，课程结束后 1 分钟内刷新"
                            } else {
                                "未授权精确闹钟；点此前往系统设置允许「闹钟和提醒」"
                            },
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier.clickable {
                        if (exactAllowed) {
                            localMessage = "已允许精确闹钟，小组件会在下课后刷新"
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }.onFailure {
                                localMessage = "无法打开授权页面，请在系统设置中允许「闹钟和提醒」"
                            }
                        }
                    },
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
                                    "当前课表每天 ${state.periodsPerDay} 节",
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
                                            showTimeTargetPicker = true
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

            item { SectionHeader("教务导入") }
            item {
                ListItem(
                    headlineContent = { Text("教务系统网址") },
                    supportingContent = {
                        Text(
                            text = eduUrl.ifBlank { "尚未设置，导入时填写后会自动记住" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = { showEduUrlDialog = true }) { Text("编辑") }
                    },
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("数据") }
            item {
                ListItem(
                    headlineContent = { Text("导出日历") },
                    supportingContent = { Text("保存为 .ics，可导入系统日历、Google 日历等") },
                    leadingContent = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier.clickable {
                        exportIcsLauncher.launch("FallAsleep课表.ics")
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("导入到日历") },
                    supportingContent = {
                        Text(
                            if (calendarBusy) {
                                "正在导入…"
                            } else {
                                "当前课表写入系统日历，提前 10 分钟提醒"
                            },
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier.clickable(enabled = !calendarBusy) {
                        withCalendarPermission { runCalendarExport() }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("删除当前课表日程") },
                    supportingContent = {
                        Text(
                            if (calendarBusy) {
                                "处理中…"
                            } else {
                                "只删除当前课表已写入系统日历的事件"
                            },
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    modifier = Modifier.clickable(enabled = !calendarBusy) {
                        deleteCurrentCalendarConfirm = true
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("删除全部已导入日程") },
                    supportingContent = {
                        Text(
                            if (calendarBusy) {
                                "处理中…"
                            } else {
                                "清空「FallAsleep课程表」日历中的所有事件"
                            },
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    modifier = Modifier.clickable(enabled = !calendarBusy) {
                        deleteAllCalendarConfirm = true
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("导出日志") },
                    supportingContent = {
                        Text("反馈解析问题用；已脱敏密码/token，仍请检查后发送")
                    },
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
                    headlineContent = { Text("联系开发者") },
                    supportingContent = { Text("点此提交问题反馈") },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.profile_avatar),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    },
                    modifier = Modifier.clickable { showContactDeveloper = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("我还想看一遍欢迎页🥺") },
                    supportingContent = { Text("重新浏览四页功能介绍") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.clickable { onShowWelcome() },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("FallAsleep 课程表") },
                    supportingContent = { Text("版本 ${BuildConfig.VERSION_NAME}") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    updateReminders = !updateReminders
                                    settingsPrefs.edit()
                                        .putBoolean("update_reminders", updateReminders)
                                        .apply()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = if (updateReminders) {
                                        "拒收更新提醒"
                                    } else {
                                        "接收更新提醒"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            TextButton(
                                onClick = { showUpdateDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = "检查更新",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("许可证") },
                    supportingContent = {
                        Text("PolyForm Noncommercial 1.0.0 · 禁止商业使用")
                    },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.clickable { showLicenseDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("第三方依赖") },
                    supportingContent = { Text("开源组件、版本与许可证全文") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.clickable { showThirdPartyNotices = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("隐私政策") },
                    supportingContent = { Text("本地数据、权限与教务导入说明") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.clickable { showPrivacyPolicy = true },
                )
            }
            item { Spacer(modifier = Modifier.width(1.dp).padding(bottom = 16.dp)) }
            }
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

    if (showTimeTargetPicker) {
        val defaultId = timeTargetId
            ?: state.selectedTimetable?.id
            ?: state.timetables.firstOrNull()?.id
        var pickedId by remember { mutableStateOf(defaultId) }
        AlertDialog(
            onDismissRequest = { showTimeTargetPicker = false },
            title = { Text("选择目标课表") },
            text = {
                Column {
                    Text(
                        text = "上课时间会保存到选中的课表，各课表互不影响。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.timetables.forEach { table ->
                        ListItem(
                            headlineContent = { Text(table.name) },
                            supportingContent = { Text("每天 ${table.periodsPerDay} 节") },
                            leadingContent = {
                                RadioButton(
                                    selected = pickedId == table.id,
                                    onClick = { pickedId = table.id },
                                )
                            },
                            modifier = Modifier.clickable { pickedId = table.id },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pickedId != null,
                    onClick = {
                        val id = pickedId ?: return@TextButton
                        showTimeTargetPicker = false
                        scope.launch {
                            timeTargetSlots = runCatching { onLoadTimeSlots(id) }
                                .getOrDefault(emptyList())
                            timeTargetId = id
                            showClassTimeDialog = true
                        }
                    },
                ) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = { showTimeTargetPicker = false }) { Text("取消") }
            },
        )
    }

    if (showClassTimeDialog) {
        val term = state.term
        val target = state.timetables.firstOrNull { it.id == timeTargetId }
        if (term != null && target != null) {
            ClassTimeSettingsDialog(
                timetableName = target.name,
                slots = timeTargetSlots,
                onDismiss = { showClassTimeDialog = false },
                onSave = { periods, slots, applyGlobally ->
                    onUpdateTimeSlots(target.id, periods, slots, applyGlobally)
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

    if (deleteCurrentCalendarConfirm) {
        AlertDialog(
            onDismissRequest = { deleteCurrentCalendarConfirm = false },
            title = { Text("删除当前课表日程？") },
            text = {
                Text(
                    "将删除「${state.selectedTimetable?.name ?: "当前课表"}」" +
                        "已导入到系统日历的日程；课表数据不受影响。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCurrentCalendarConfirm = false
                        withCalendarPermission { runDeleteCurrentCalendar() }
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCurrentCalendarConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (deleteAllCalendarConfirm) {
        AlertDialog(
            onDismissRequest = { deleteAllCalendarConfirm = false },
            title = { Text("删除全部已导入日程？") },
            text = {
                Text(
                    "将清空系统日历「FallAsleep课程表」中的所有事件；课表数据不受影响。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteAllCalendarConfirm = false
                        withCalendarPermission { runDeleteAllCalendar() }
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAllCalendarConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showLicenseDialog) {
        LicenseDialog(
            context = context,
            onDismiss = { showLicenseDialog = false },
        )
    }

    if (showThirdPartyNotices) {
        ThirdPartyNoticesDialog(
            context = context,
            onDismiss = { showThirdPartyNotices = false },
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(
            context = context,
            onDismiss = { showPrivacyPolicy = false },
        )
    }

    if (showContactDeveloper) {
        ContactDeveloperDialog(
            onDismiss = { showContactDeveloper = false },
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            onDismiss = { showUpdateDialog = false },
        )
    }

    if (showEduUrlDialog) {
        var urlInput by remember { mutableStateOf(eduUrl) }
        AlertDialog(
            onDismissRequest = { showEduUrlDialog = false },
            title = { Text("教务系统网址") },
            text = {
                Column {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("网址") },
                        placeholder = { Text("https://jwgl.example.edu.cn") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "导入课表时会自动填入这个网址，可随时修改或清空。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val raw = urlInput.trim()
                        val normalized = when {
                            raw.isEmpty() -> ""
                            raw.startsWith("http://") || raw.startsWith("https://") -> raw
                            else -> "http://$raw"
                        }
                        settingsPrefs.edit().putString("edu_url", normalized).apply()
                        eduUrl = normalized
                        showEduUrlDialog = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEduUrlDialog = false }) { Text("取消") }
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

private fun Long.toHexLabel(): String = String.format("#%06X", this and 0xFFFFFFL)

private fun hsvToThemeColor(hue: Float, saturation: Float, value: Float): Long {
    val argb = AndroidColor.HSVToColor(
        floatArrayOf(
            ((hue % 360f) + 360f) % 360f,
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f),
        ),
    )
    return 0xFF000000L or (argb.toLong() and 0xFFFFFFL)
}

@Composable
private fun ThemeColorDialog(
    initialColor: Long?,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    val initial = initialColor ?: DEFAULT_THEME_SEED
    val initialHsv = remember(initial) {
        FloatArray(3).also { AndroidColor.colorToHSV(initial.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(initial.toHexLabel()) }
    var hexError by remember { mutableStateOf(false) }

    val selected = hsvToThemeColor(hue, saturation, value)
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val previewScheme = remember(selected, darkTheme) {
        colorSchemeFromSeed(Color(selected), darkTheme)
    }

    fun pick(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        value = v
        hexText = hsvToThemeColor(h, s, v).toHexLabel()
        hexError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义主题色") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "拖动色环选颜色，再调整明度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HueSaturationWheel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(196.dp),
                    onPick = { h, s -> pick(h, s, value) },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("明度", style = MaterialTheme.typography.labelLarge)
                    BrightnessSlider(
                        hue = hue,
                        saturation = saturation,
                        value = value,
                        onValueChange = { pick(hue, saturation, it) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(value * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 用生成的语义色画一个迷你课表卡片，直接看浅色/深色下的层级。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(previewScheme.surfaceContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "课程表",
                            style = MaterialTheme.typography.titleSmall,
                            color = previewScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = selected.toHexLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = previewScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "高等数学 · 教三 201",
                        style = MaterialTheme.typography.bodySmall,
                        color = previewScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreviewChip("今天", previewScheme.primary, previewScheme.onPrimary)
                        PreviewChip(
                            "周三",
                            previewScheme.secondaryContainer,
                            previewScheme.onSecondaryContainer,
                        )
                        PreviewChip(
                            "第 3 节",
                            previewScheme.tertiaryContainer,
                            previewScheme.onTertiaryContainer,
                        )
                    }
                }

                Text(
                    text = "Material 调色板",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidThemePalettes.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { preset ->
                            val isSelected =
                                (selected and 0xFFFFFFL) == (preset.seed and 0xFFFFFFL)
                            val presetColor = Color(preset.seed)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClickLabel = "使用${preset.name}") {
                                        val presetHsv = FloatArray(3)
                                        AndroidColor.colorToHSV(preset.seed.toInt(), presetHsv)
                                        pick(presetHsv[0], presetHsv[1], presetHsv[2])
                                    }
                                    .semantics {
                                        contentDescription = preset.name
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(presetColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (presetColor.luminance() > 0.5f) {
                                                Color.Black
                                            } else {
                                                Color.White
                                            },
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val parsed = parseHexColor(input)
                        if (parsed != null) {
                            val typedHsv = FloatArray(3)
                            AndroidColor.colorToHSV(parsed.toInt(), typedHsv)
                            // 黑白没有色相/饱和度，保留上一次的取值，方便再调回来。
                            if (typedHsv[2] > 0.001f) value = typedHsv[2]
                            if (typedHsv[1] > 0.001f) saturation = typedHsv[1]
                            if (typedHsv[1] > 0.001f && typedHsv[2] > 0.001f) {
                                hue = typedHsv[0]
                            }
                            hexError = false
                        } else {
                            hexError = true
                        }
                    },
                    label = { Text("十六进制颜色") },
                    singleLine = true,
                    isError = hexError,
                    supportingText = {
                        Text(if (hexError) "格式应为 #RRGGBB" else "也可以直接输入 #RRGGBB")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
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

@Composable
private fun PreviewChip(label: String, background: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/** HSV 色环：角度选色相，半径选饱和度，整体按明度压暗。 */
@Composable
private fun HueSaturationWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    modifier: Modifier = Modifier,
    onPick: (hue: Float, saturation: Float) -> Unit,
) {
    val currentOnPick by rememberUpdatedState(onPick)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = "色环，拖动选择颜色" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    fun pick(position: Offset) {
                        val radius = minOf(size.width, size.height) / 2f
                        if (radius <= 0f) return
                        val dx = position.x - size.width / 2f
                        val dy = position.y - size.height / 2f
                        val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
                        val degrees = Math.toDegrees(
                            atan2(dy.toDouble(), dx.toDouble()),
                        ).toFloat()
                        currentOnPick((degrees + 360f) % 360f, distance / radius)
                    }

                    val down = awaitFirstDown()
                    pick(down.position)
                    down.consume()
                    var event = awaitPointerEvent()
                    while (event.changes.any { it.pressed }) {
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        pick(change.position)
                        change.consume()
                        event = awaitPointerEvent()
                    }
                }
            },
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
                center = center,
            ),
            radius = radius,
            center = center,
        )
        // 圆心趋白，模拟饱和度 0 → 1。
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        if (value < 1f) {
            drawCircle(
                color = Color.Black.copy(alpha = 1f - value),
                radius = radius,
                center = center,
            )
        }
        val angle = Math.toRadians(hue.toDouble())
        val distance = saturation.coerceIn(0f, 1f) * radius
        val marker = Offset(
            center.x + cos(angle).toFloat() * distance,
            center.y + sin(angle).toFloat() * distance,
        )
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = marker,
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = 10.5.dp.toPx(),
            center = marker,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** 明度滑杆：黑 → 当前色相/饱和度的最亮色。 */
@Composable
private fun BrightnessSlider(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val brightColor = remember(hue, saturation) {
        Color(hsvToThemeColor(hue, saturation.coerceAtLeast(0.05f), 1f))
    }
    val trackOutline = MaterialTheme.colorScheme.outlineVariant
    val thumbFill = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .height(48.dp)
            .semantics { contentDescription = "明度，左右拖动调整" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val thumbRadius = 10.dp.toPx()
                    val usable = (size.width - thumbRadius * 2f).coerceAtLeast(1f)

                    fun pick(position: Offset) {
                        currentOnValueChange(
                            ((position.x - thumbRadius) / usable).coerceIn(0f, 1f),
                        )
                    }

                    val down = awaitFirstDown()
                    pick(down.position)
                    down.consume()
                    var event = awaitPointerEvent()
                    while (event.changes.any { it.pressed }) {
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        pick(change.position)
                        change.consume()
                        event = awaitPointerEvent()
                    }
                }
            },
    ) {
        val trackHeight = 18.dp.toPx()
        val top = (size.height - trackHeight) / 2f
        val radius = trackHeight / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, brightColor),
                startX = 0f,
                endX = size.width,
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(radius),
        )
        drawRoundRect(
            color = trackOutline,
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = 1.dp.toPx()),
        )

        val thumbRadius = 10.dp.toPx()
        val thumbX = value.coerceIn(0f, 1f) * (size.width - thumbRadius * 2f) + thumbRadius
        val thumbCenter = Offset(thumbX, size.height / 2f)
        drawCircle(color = thumbFill, radius = thumbRadius, center = thumbCenter)
        drawCircle(
            color = trackOutline,
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(color = brightColor, radius = thumbRadius * 0.45f, center = thumbCenter)
    }
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
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
