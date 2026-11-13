package io.github.fallasleeptimetable.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fallasleeptimetable.R
import io.github.fallasleeptimetable.ui.screens.CourseEditorDialog
import io.github.fallasleeptimetable.ui.screens.CreateTimetableDialog
import io.github.fallasleeptimetable.ui.screens.EduImportActivity
import io.github.fallasleeptimetable.ui.screens.EditorRequest
import io.github.fallasleeptimetable.ui.screens.ScheduleScreen
import io.github.fallasleeptimetable.ui.screens.SettingsScreen
import io.github.fallasleeptimetable.ui.screens.ShareExportSheet
import io.github.fallasleeptimetable.ui.screens.TimetablePickerDialog
import io.github.fallasleeptimetable.ui.screens.TodayScreen
import kotlinx.coroutines.delay

private enum class AppDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Schedule(R.string.nav_schedule, Icons.Filled.DateRange),
    Today(R.string.nav_today, Icons.Filled.Home),
    Settings(R.string.nav_settings, Icons.Filled.Settings),
}

@Composable
fun FallAsleepApp(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    customColorArgb: Long?,
    onCustomColorPicked: (Long) -> Unit,
    hideStatusBar: Boolean,
    onHideStatusBarChange: (Boolean) -> Unit,
    onShowWelcome: () -> Unit,
) {
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val state by scheduleViewModel.uiState.collectAsStateWithLifecycle()
    val message by scheduleViewModel.message.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(AppDestination.Schedule) }
    var fabExpanded by remember { mutableStateOf(false) }
    var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }
    var showTimetablePicker by remember { mutableStateOf(false) }
    var showCreateTimetable by remember { mutableStateOf(false) }
    var eduMessage by remember { mutableStateOf<String?>(null) }
    var showEduUrlDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var showShareSheet by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }

    // 预测式返回：全屏时退出全屏；在「今日/设置」时回到「课表」。
    // 手势过程中按进度位移、淡出当前页面，取消则自动回弹。
    PredictiveBackHandler(
        enabled = fullscreen || destination != AppDestination.Schedule,
    ) { progress ->
        var completed = false
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            completed = true
            if (fullscreen) {
                fullscreen = false
            } else {
                destination = AppDestination.Schedule
            }
        } finally {
            // 取消时立即回弹；完成时保留当前进度，让上层按滑开位置淡出，
            // 避免复位瞬间把页面弹回盖住课表造成闪烁。
            if (!completed) {
                backProgress = 0f
            }
        }
    }

    // 退出动画结束后再把预测进度归零（包括全屏返回后 destination 不变的情况）。
    LaunchedEffect(destination, fullscreen) {
        if (!fullscreen && destination == AppDestination.Schedule && backProgress > 0f) {
            delay(240)
            backProgress = 0f
        }
    }

    val view = LocalView.current
    DisposableEffect(fullscreen, hideStatusBar) {
        val controller = view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
        }
        if (controller != null) {
            if (fullscreen) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                if (hideStatusBar) {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
        onDispose { /* 系统栏的真正恢复交给下面的 DisposableEffect(view) */ }
    }

    // 只有离开本组合时才恢复全部系统栏；切换全屏/开关时不要在 onDispose
    // 里先 show 一遍，否则退出全屏会闪一下状态栏。
    DisposableEffect(view) {
        val controller = view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val context = LocalContext.current
    val settingsPrefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var eduUrlInput by remember {
        mutableStateOf(settingsPrefs.getString("edu_url", "") ?: "")
    }
    val eduImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = result.data?.getLongExtra(EduImportActivity.EXTRA_TIMETABLE_ID, -1L)
                ?: -1L
            if (id > 0) {
                scheduleViewModel.selectTimetable(id)
                val name = result.data
                    ?.getStringExtra(EduImportActivity.EXTRA_TIMETABLE_NAME)
                    ?.takeIf { it.isNotBlank() }
                    ?: "导入的课表"
                renameTarget = id to name
            }
        }
    }

    LaunchedEffect(destination, state.selectedTimetable?.id) {
        fabExpanded = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp

        if (compact) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AppContent(
                        destination = destination,
                        state = state,
                        viewModel = scheduleViewModel,
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = onDynamicColorChange,
                        customColorArgb = customColorArgb,
                        onCustomColorPicked = onCustomColorPicked,
                        fullscreen = fullscreen,
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        onEditRequest = { editorRequest = it },
                        onCreateTimetableRequest = { showCreateTimetable = true },
                        message = message,
                        onConsumeMessage = scheduleViewModel::consumeMessage,
                        hideStatusBar = hideStatusBar,
                        onHideStatusBarChange = onHideStatusBarChange,
                        onShowWelcome = onShowWelcome,
                        onShareClick = { showShareSheet = true },
                        backProgress = backProgress,
                    )
                }
                if (!fullscreen) {
                    NavigationBar {
                        AppDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                            )
                        }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                if (!fullscreen) {
                Surface(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        AppDestination.entries.forEach { item ->
                            NavigationRailItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AppContent(
                        destination = destination,
                        state = state,
                        viewModel = scheduleViewModel,
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = onDynamicColorChange,
                        customColorArgb = customColorArgb,
                        onCustomColorPicked = onCustomColorPicked,
                        fullscreen = fullscreen,
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        onEditRequest = { editorRequest = it },
                        onCreateTimetableRequest = { showCreateTimetable = true },
                        message = message,
                        onConsumeMessage = scheduleViewModel::consumeMessage,
                        hideStatusBar = hideStatusBar,
                        onHideStatusBarChange = onHideStatusBarChange,
                        onShowWelcome = onShowWelcome,
                        onShareClick = { showShareSheet = true },
                        backProgress = backProgress,
                    )
                }
            }
        }

        if (!fullscreen && destination == AppDestination.Schedule &&
            state.selectedTimetable != null
        ) {
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { fabExpanded = false },
                )
            }

            ScheduleFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                onAddCourse = {
                    fabExpanded = false
                    state.term?.let { term ->
                        editorRequest = EditorRequest(
                            dayOfWeek = 1,
                            startPeriod = 1,
                            periodCount = 2,
                            startWeek = 1,
                            endWeek = term.totalWeeks,
                            colorIndex = state.courses.size % 6,
                        )
                    }
                },
                onEduImport = {
                    fabExpanded = false
                    if (state.term == null) {
                        eduMessage = "还没有学期，无法导入"
                    } else {
                        showEduUrlDialog = true
                    }
                },
                onSwitchTimetable = {
                    fabExpanded = false
                    showTimetablePicker = true
                },
                onCreateTimetable = {
                    fabExpanded = false
                    showCreateTimetable = true
                },
                onJumpToCurrentWeek = scheduleViewModel::jumpToCurrentWeek,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = if (compact) 96.dp else 16.dp),
            )
        }
    }

    editorRequest?.let { request ->
        state.term?.let { term ->
            CourseEditorDialog(
                request = request,
                term = term,
                onDismiss = { editorRequest = null },
                onSave = { draft ->
                    scheduleViewModel.saveCourse(request.courseId, request.sessionId, draft)
                    editorRequest = null
                },
            )
        }
    }

    if (showTimetablePicker) {
        TimetablePickerDialog(
            state = state,
            onDismiss = { showTimetablePicker = false },
            onSelect = { id ->
                scheduleViewModel.selectTimetable(id)
                showTimetablePicker = false
            },
        )
    }

    if (showCreateTimetable) {
        CreateTimetableDialog(
            onDismiss = { showCreateTimetable = false },
            onCreate = { name ->
                scheduleViewModel.createTimetable(name)
                showCreateTimetable = false
            },
        )
    }

    if (showEduUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEduUrlDialog = false },
            title = { Text("从教务系统导入") },
            text = {
                Column {
                    OutlinedTextField(
                        value = eduUrlInput,
                        onValueChange = { eduUrlInput = it },
                        label = { Text("教务网址") },
                        placeholder = { Text("https://jwgl.example.edu.cn") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "打开后在内嵌浏览器里手动登录，再点右上角「抓取本页」。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val raw = eduUrlInput.trim()
                        if (raw.isEmpty()) {
                            eduMessage = "请输入教务网址"
                        } else {
                            val normalized = if (
                                raw.startsWith("http://") || raw.startsWith("https://")
                            ) {
                                raw
                            } else {
                                "http://$raw"
                            }
                            eduUrlInput = normalized
                            settingsPrefs.edit().putString("edu_url", normalized).apply()
                            showEduUrlDialog = false
                            state.term?.let { term ->
                                eduImportLauncher.launch(
                                    Intent(context, EduImportActivity::class.java)
                                        .putExtra(EduImportActivity.EXTRA_TERM_ID, term.id)
                                        .putExtra(EduImportActivity.EXTRA_URL, normalized),
                                )
                            }
                        }
                    },
                    enabled = eduUrlInput.isNotBlank(),
                ) { Text("打开") }
            },
            dismissButton = {
                TextButton(onClick = { showEduUrlDialog = false }) { Text("取消") }
            },
        )
    }

    renameTarget?.let { (timetableId, initialName) ->
        var nameInput by remember(timetableId) { mutableStateOf(initialName) }

        AlertDialog(
            onDismissRequest = {
                renameTarget = null
                eduMessage = "已从教务系统导入"
            },
            title = { Text("课表已导入") },
            text = {
                Column {
                    Text("给新课表起个名字：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("课表名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = nameInput.trim()
                        if (name.isNotEmpty()) {
                            scheduleViewModel.renameTimetable(timetableId, name)
                        }
                        renameTarget = null
                        eduMessage = "已从教务系统导入"
                    },
                    enabled = nameInput.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameTarget = null
                        eduMessage = "已从教务系统导入"
                    },
                ) { Text("稍后") }
            },
        )
    }

    eduMessage?.let { text ->
        AlertDialog(
            onDismissRequest = { eduMessage = null },
            title = { Text("提示") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { eduMessage = null }) { Text("好") }
            },
        )
    }

    if (showShareSheet) {
        ShareExportSheet(
            state = state,
            viewModel = scheduleViewModel,
            onDismiss = { showShareSheet = false },
            onMessage = { shareMessage = it },
        )
    }

    shareMessage?.let { text ->
        AlertDialog(
            onDismissRequest = { shareMessage = null },
            title = { Text("提示") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { shareMessage = null }) { Text("好") }
            },
        )
    }
}

@Composable
private fun AppContent(
    destination: AppDestination,
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    customColorArgb: Long?,
    onCustomColorPicked: (Long) -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onEditRequest: (EditorRequest) -> Unit,
    onCreateTimetableRequest: () -> Unit,
    message: String?,
    onConsumeMessage: () -> Unit,
    hideStatusBar: Boolean,
    onHideStatusBarChange: (Boolean) -> Unit,
    onShowWelcome: () -> Unit,
    onShareClick: () -> Unit,
    backProgress: Float,
) {
    val scheduleContent: @Composable () -> Unit = {
        ScheduleScreen(
            state = state,
            onPrevWeek = viewModel::previousWeek,
            onNextWeek = viewModel::nextWeek,
            onJumpToCurrentWeek = viewModel::jumpToCurrentWeek,
            fullscreen = fullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onDeleteCourse = viewModel::deleteCourse,
            onEditRequest = onEditRequest,
            onCreateTimetableRequest = onCreateTimetableRequest,
            onShareClick = onShareClick,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 课表始终垫在最下层，今日/设置通过预测式返回滑开时可以真实露出课表。
        Box(
            modifier = if (destination == AppDestination.Schedule) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize().clearAndSetSemantics {}
            },
        ) {
            scheduleContent()
        }

        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(120))
            },
            label = "destination",
        ) { dest ->
            if (dest != AppDestination.Schedule) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = backProgress * size.width * 0.25f
                            alpha = 1f - backProgress * 0.15f
                        },
                ) {
                    when (dest) {
                        AppDestination.Today -> TodayScreen(state = state)
                        AppDestination.Settings -> SettingsScreen(
                            state = state,
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = onDynamicColorChange,
                            customColorArgb = customColorArgb,
                            onCustomColorPicked = onCustomColorPicked,
                            hideStatusBar = hideStatusBar,
                            onHideStatusBarChange = onHideStatusBarChange,
                            onShowWelcome = onShowWelcome,
                            onUpdateTerm = viewModel::updateTerm,
                            onUpdateTimeSlots = viewModel::updateTimeSlots,
                            onSelectTimetable = viewModel::selectTimetable,
                            onCreateTimetable = viewModel::createTimetable,
                            onDeleteTimetable = viewModel::deleteTimetable,
                            onRenameTimetable = viewModel::renameTimetable,
                            onExportIcs = viewModel::exportTimetableIcs,
                            message = message,
                            onConsumeMessage = onConsumeMessage,
                        )
                        AppDestination.Schedule -> Unit
                    }
                }
            }
        }
    }

}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
