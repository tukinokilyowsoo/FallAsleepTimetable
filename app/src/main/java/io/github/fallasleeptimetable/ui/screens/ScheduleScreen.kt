package io.github.fallasleeptimetable.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.fallasleeptimetable.R
import io.github.fallasleeptimetable.data.CourseSearchRow
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.domain.CourseDraft
import io.github.fallasleeptimetable.domain.VisibleSession
import io.github.fallasleeptimetable.domain.assignLanes
import io.github.fallasleeptimetable.domain.visibleSessions
import io.github.fallasleeptimetable.ui.ScheduleUiState
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.dayShortLabel
import io.github.fallasleeptimetable.ui.formatMinutes
import io.github.fallasleeptimetable.ui.periodLabel
import io.github.fallasleeptimetable.ui.weekRangeLabel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val weekDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")
private val timeColumnWidth = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToCurrentWeek: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onEditRequest: (EditorRequest) -> Unit,
    onCreateTimetableRequest: () -> Unit,
    searchQuery: String,
    searchResults: List<CourseSearchRow>,
    onSearchCourses: (String) -> Unit,
    onSearchResultClick: (CourseSearchRow) -> Unit,
) {
    val term = state.term
    var detailSession by remember { mutableStateOf<VisibleSession?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    val fullscreenZoom = remember(fullscreen) {
        Animatable(if (fullscreen) 0.94f else 1f)
    }
    LaunchedEffect(fullscreenZoom) {
        fullscreenZoom.animateTo(
            targetValue = 1f,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
        )
    }
    val haptics = LocalHapticFeedback.current
    val prevWeekWithHaptic = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onPrevWeek()
    }
    val nextWeekWithHaptic = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onNextWeek()
    }

    Scaffold(
        topBar = {
            if (!fullscreen) {
                TopAppBar(
                    title = { Text(state.selectedTimetable?.name ?: "课表") },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "搜索课程",
                            )
                        }
                    },
                )
            }
        },
        contentWindowInsets = if (fullscreen) {
            WindowInsets(0, 0, 0, 0)
        } else {
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = if (fullscreen) 12.dp else 0.dp)
                .padding(horizontal = 16.dp),
        ) {
            when {
                !state.ready -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                term == null || state.selectedTimetable == null -> {
                    NoTimetableState(
                        modifier = Modifier.align(Alignment.Center),
                        onCreate = onCreateTimetableRequest,
                    )
                }

                else -> {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = fullscreenZoom.value
                                scaleY = fullscreenZoom.value
                            },
                    ) {
                        val expanded = maxWidth >= 840.dp
                        if (expanded && !fullscreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                AnimatedContent(
                                    targetState = state.currentWeek,
                                    modifier = Modifier.weight(2f).fillMaxHeight(),
                                    transitionSpec = { slideWeekBoard() },
                                    label = "weekBoardTransition",
                                ) { animatedWeek ->
                                    val animatedState = state.forWeek(animatedWeek)
                                    val sessions = remember(animatedWeek, state.courses) {
                                        visibleSessions(state.courses, animatedWeek)
                                    }
                                    WeekBoard(
                                        state = animatedState,
                                        sessions = sessions,
                                        onPrevWeek = prevWeekWithHaptic,
                                        onNextWeek = nextWeekWithHaptic,
                                        onJumpToCurrentWeek = onJumpToCurrentWeek,
                                        fullscreen = false,
                                        onToggleFullscreen = onToggleFullscreen,
                                        onEmptyClick = { day, period ->
                                            onEditRequest(
                                                EditorRequest(
                                                    dayOfWeek = day,
                                                    startPeriod = period,
                                                    periodCount = 2.coerceAtMost(
                                                        term.periodsPerDay - period + 1,
                                                    ),
                                                    startWeek = 1,
                                                    endWeek = term.totalWeeks,
                                                    colorIndex = animatedState.courses.size % 6,
                                                ),
                                            )
                                        },
                                        onSessionClick = { detailSession = it },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                AnimatedContent(
                                    targetState = state.currentWeek,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    transitionSpec = { fadeWeekOverview() },
                                    label = "weekOverviewTransition",
                                ) { animatedWeek ->
                                    val animatedState = state.forWeek(animatedWeek)
                                    val sessions = remember(animatedWeek, state.courses) {
                                        visibleSessions(state.courses, animatedWeek)
                                    }
                                    WeekOverview(
                                        state = animatedState,
                                        sessions = sessions,
                                        onSessionClick = { detailSession = it },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        } else {
                            AnimatedContent(
                                targetState = state.currentWeek,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = { slideWholeWeek() },
                                label = "weekTransition",
                            ) { animatedWeek ->
                                val animatedState = state.forWeek(animatedWeek)
                                val sessions = remember(animatedWeek, state.courses) {
                                    visibleSessions(state.courses, animatedWeek)
                                }
                                WeekBoard(
                                    state = animatedState,
                                    sessions = sessions,
                                    onPrevWeek = prevWeekWithHaptic,
                                    onNextWeek = nextWeekWithHaptic,
                                    onJumpToCurrentWeek = onJumpToCurrentWeek,
                                    fullscreen = fullscreen,
                                    onToggleFullscreen = onToggleFullscreen,
                                    onEmptyClick = { day, period ->
                                        onEditRequest(
                                            EditorRequest(
                                                dayOfWeek = day,
                                                startPeriod = period,
                                                periodCount = 2.coerceAtMost(
                                                    term.periodsPerDay - period + 1,
                                                ),
                                                startWeek = 1,
                                                endWeek = term.totalWeeks,
                                                colorIndex = animatedState.courses.size % 6,
                                            ),
                                        )
                                    },
                                    onSessionClick = { detailSession = it },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailSession?.let { session ->
        CourseDetailDialog(
            session = session,
            onEdit = {
                onEditRequest(
                    EditorRequest(
                        courseId = session.courseId,
                        sessionId = session.sessionId,
                        name = session.name,
                        teacher = session.teacher ?: "",
                        room = session.room ?: "",
                        dayOfWeek = session.dayOfWeek,
                        startPeriod = session.startPeriod,
                        periodCount = session.periodCount,
                        startWeek = session.startWeek,
                        endWeek = session.endWeek,
                        parity = session.parity,
                        colorIndex = session.colorIndex,
                    ),
                )
                detailSession = null
            },
            onDelete = {
                onDeleteCourse(session.courseId)
                detailSession = null
            },
            onDismiss = { detailSession = null },
        )
    }

    if (showSearch) {
        CourseSearchDialog(
            query = searchQuery,
            results = searchResults,
            onQueryChange = onSearchCourses,
            onResultClick = { row ->
                onSearchResultClick(row)
                showSearch = false
            },
            onDismiss = {
                showSearch = false
                onSearchCourses("")
            },
        )
    }
}

@Composable
private fun CourseSearchDialog(
    query: String,
    results: List<CourseSearchRow>,
    onQueryChange: (String) -> Unit,
    onResultClick: (CourseSearchRow) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索课程") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("课程名、教师或教室") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    query.isBlank() -> Text(
                        text = "输入关键词后显示结果，点击结果跳转到对应课表与周次。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    results.isEmpty() -> Text(
                        text = "没有匹配的课程",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(results) { row ->
                            val colors = courseColors(row.colorIndex)
                            ListItem(
                                headlineContent = { Text(row.courseName) },
                                overlineContent = { Text(row.timetableName) },
                                supportingContent = {
                                    Text(
                                        buildString {
                                            append(dayLabel(row.dayOfWeek)).append(' ')
                                            append(
                                                periodLabel(
                                                    row.startPeriod,
                                                    row.periodCount,
                                                ),
                                            )
                                            append(" · ")
                                            append(
                                                weekRangeLabel(
                                                    row.startWeek,
                                                    row.endWeek,
                                                    row.parity,
                                                ),
                                            )
                                            row.teacher?.takeIf { it.isNotBlank() }?.let {
                                                append(" · ").append(it)
                                            }
                                            row.room?.takeIf { it.isNotBlank() }?.let {
                                                append(" · ").append(it)
                                            }
                                        },
                                    )
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colors.container),
                                    )
                                },
                                modifier = Modifier.clickable { onResultClick(row) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

private fun AnimatedContentTransitionScope<Int>.slideWeekBoard(): ContentTransform {
    val direction = if (targetState > initialState) 1 else -1
    val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
        durationMillis = 240,
        easing = FastOutSlowInEasing,
    )
    return (
        slideInHorizontally(slideSpec) { width -> direction * width / 4 } +
            fadeIn(tween(220))
        ).togetherWith(
        slideOutHorizontally(slideSpec) { width -> -direction * width / 4 } +
            fadeOut(tween(150)),
    )
}

private fun AnimatedContentTransitionScope<Int>.fadeWeekOverview(): ContentTransform {
    val direction = if (targetState > initialState) 1 else -1
    val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
        durationMillis = 200,
        easing = FastOutSlowInEasing,
    )
    return (
        slideInHorizontally(slideSpec) { width -> direction * width / 10 } +
            fadeIn(tween(200, delayMillis = 30))
        ).togetherWith(
        slideOutHorizontally(slideSpec) { width -> -direction * width / 10 } +
            fadeOut(tween(120)),
    )
}

private fun AnimatedContentTransitionScope<Int>.slideWholeWeek(): ContentTransform {
    val direction = if (targetState > initialState) 1 else -1
    val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
        durationMillis = 220,
        easing = FastOutSlowInEasing,
    )
    return (
        slideInHorizontally(slideSpec) { width -> direction * width / 5 } +
            fadeIn(tween(220))
        ).togetherWith(
        slideOutHorizontally(slideSpec) { width -> -direction * width / 5 } +
            fadeOut(tween(160)),
    )
}

private fun ScheduleUiState.forWeek(week: Int): ScheduleUiState {
    val start = term?.let { LocalDate.ofEpochDay(it.startDateEpochDay) } ?: return this
    return copy(
        currentWeek = week,
        isCurrentWeek = week == actualWeek,
        weekStart = start.plusWeeks((week - 1).toLong()),
    )
}

@Composable
private fun WeekBoard(
    state: ScheduleUiState,
    sessions: List<VisibleSession>,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToCurrentWeek: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onEmptyClick: (Int, Int) -> Unit,
    onSessionClick: (VisibleSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val term = state.term ?: return
    val weekStart = state.weekStart
    val days = remember(term.showSaturday, term.showSunday) {
        buildList {
            addAll(1..5)
            if (term.showSaturday) add(6)
            if (term.showSunday) add(7)
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("第 ${state.currentWeek} 周", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = weekStart?.let { start ->
                            val end = start.plusDays(6)
                            "${start.format(weekDateFormat)} – ${end.format(weekDateFormat)}"
                        } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.isCurrentWeek) {
                    TextButton(onClick = onJumpToCurrentWeek) { Text("本周") }
                }
                FilledTonalIconButton(onClick = onToggleFullscreen) {
                    Icon(
                        painter = painterResource(
                            if (fullscreen) {
                                R.drawable.ic_fullscreen_exit
                            } else {
                                R.drawable.ic_fullscreen
                            },
                        ),
                        contentDescription = if (fullscreen) "退出全屏" else "全屏",
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onPrevWeek) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一周")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onNextWeek) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一周")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val compact = maxWidth < 600.dp
                val timeWidth = if (compact) 40.dp else timeColumnWidth
                val horizontalState = rememberScrollState()
                val verticalState = rememberScrollState()
                val headerHeight = 44.dp
                val rowHeight = ((maxHeight - headerHeight) / term.periodsPerDay)
                    .coerceIn(40.dp, 72.dp)
                val availableDayWidth = (maxWidth - timeWidth) / days.size
                val dayWidth = if (compact) {
                    maxOf(availableDayWidth, 64.dp)
                } else {
                    availableDayWidth
                }
                val contentWidth = timeWidth + dayWidth * days.size

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalState),
                ) {
                    Column(modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                        Row(modifier = Modifier.fillMaxWidth().height(headerHeight)) {
                            Spacer(modifier = Modifier.width(timeWidth))
                            days.forEach { day ->
                                DayHeader(
                                    day = day,
                                    date = weekStart?.plusDays((day - 1).toLong()),
                                    isToday = day == state.todayDayOfWeek && state.isCurrentWeek,
                                    modifier = Modifier.width(dayWidth).fillMaxHeight(),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(verticalState),
                        ) {
                            Row(
                                modifier = Modifier
                                    .width(contentWidth)
                                    .height(rowHeight * term.periodsPerDay),
                            ) {
                                TimeColumn(
                                    periods = term.periodsPerDay,
                                    rowHeight = rowHeight,
                                    slots = state.timeSlots,
                                    width = timeWidth,
                                )
                                days.forEach { day ->
                                    val daySessions = sessions
                                        .filter { it.dayOfWeek == day }
                                        .sortedWith(
                                            compareBy({ it.startPeriod }, { it.sessionId }),
                                        )
                                    DayColumn(
                                        day = day,
                                        periods = term.periodsPerDay,
                                        rowHeight = rowHeight,
                                        sessions = daySessions,
                                        isToday = day == state.todayDayOfWeek &&
                                            state.isCurrentWeek,
                                        compact = compact,
                                        onEmptyClick = { period -> onEmptyClick(day, period) },
                                        onSessionClick = onSessionClick,
                                        modifier = Modifier.width(dayWidth).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    day: Int,
    date: LocalDate?,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = dayShortLabel(day),
            style = MaterialTheme.typography.labelLarge,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        date?.let {
            Text(
                text = it.format(weekDateFormat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimeColumn(
    periods: Int,
    rowHeight: Dp,
    slots: List<io.github.fallasleeptimetable.data.TimeSlotEntity>,
    width: Dp,
) {
    Column(modifier = Modifier.width(width).fillMaxHeight()) {
        repeat(periods) { index ->
            val period = index + 1
            val slot = slots.firstOrNull { it.period == period }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = period.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (slot != null) {
                    Text(
                        text = formatMinutes(slot.startMinute),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    day: Int,
    periods: Int,
    rowHeight: Dp,
    sessions: List<VisibleSession>,
    isToday: Boolean,
    compact: Boolean,
    onEmptyClick: (Int) -> Unit,
    onSessionClick: (VisibleSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val todayTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    val (lanes, laneCount) = remember(sessions) { assignLanes(sessions) }

    BoxWithConstraints(modifier = modifier) {
        val dayWidth = maxWidth
        if (isToday) {
            Box(modifier = Modifier.matchParentSize().background(todayTint))
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val step = rowHeight.toPx()
            for (index in 0..periods) {
                val y = step * index
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
        }

        Column(modifier = Modifier.matchParentSize()) {
            repeat(periods) { index ->
                val period = index + 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onEmptyClick(period) }
                        .semantics {
                            contentDescription =
                                "${dayLabel(day)}第 $period 节，添加课程"
                            role = Role.Button
                        },
                )
            }
        }

        sessions.forEach { session ->
            val lane = lanes[session.sessionId] ?: 0
            val colors = courseColors(session.colorIndex)
            val blockWidth = dayWidth / laneCount
            val description = buildString {
                append(session.name)
                append("，").append(dayLabel(session.dayOfWeek))
                append(" ").append(periodLabel(session.startPeriod, session.periodCount))
                append("，")
                append(weekRangeLabel(session.startWeek, session.endWeek, session.parity))
                session.room?.takeIf { it.isNotBlank() }?.let {
                    append("，").append(it)
                }
                session.teacher?.takeIf { it.isNotBlank() }?.let {
                    append("，").append(it)
                }
            }
            Box(
                modifier = Modifier
                    .offset(
                        x = blockWidth * lane,
                        y = rowHeight * (session.startPeriod - 1),
                    )
                    .width(blockWidth)
                    .height(rowHeight * session.periodCount)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.container)
                    .clickable { onSessionClick(session) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        role = Role.Button
                    }
                    .padding(
                        horizontal = if (compact) 3.dp else 6.dp,
                        vertical = if (compact) 2.dp else 4.dp,
                    ),
            ) {
                Column {
                    Text(
                        text = session.name,
                        style = if (compact) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.labelMedium
                        },
                        color = colors.content,
                        maxLines = if (compact) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!compact || blockWidth >= 64.dp) {
                        session.room?.let { room ->
                            Text(
                                text = room,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.content.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekOverview(
    state: ScheduleUiState,
    sessions: List<VisibleSession>,
    onSessionClick: (VisibleSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }

    val todaySessions = remember(sessions, state.todayDayOfWeek) {
        sessions
            .filter { it.dayOfWeek == state.todayDayOfWeek }
            .sortedBy { it.startPeriod }
    }
    val nowMinutes = now.hour * 60 + now.minute

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "今日课程",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "第 ${state.currentWeek} 周 · ${dayLabel(state.todayDayOfWeek)} · " +
                    "${todaySessions.size} 节课",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (todaySessions.isEmpty()) {
                Text(
                    text = "今天没有课程，点击课表空格或右下角按钮添加。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        todaySessions,
                        key = { it.sessionId },
                    ) { session ->
                        val endPeriod = session.startPeriod + session.periodCount - 1
                        val endSlot = state.timeSlots.firstOrNull { it.period == endPeriod }
                        val finished = state.isCurrentWeek &&
                            endSlot != null &&
                            nowMinutes >= endSlot.endMinute
                        val description = buildString {
                            append(session.name)
                            append("，").append(dayLabel(session.dayOfWeek))
                            append(" ")
                            append(periodLabel(session.startPeriod, session.periodCount))
                            append("，")
                            append(
                                weekRangeLabel(
                                    session.startWeek,
                                    session.endWeek,
                                    session.parity,
                                ),
                            )
                            session.room?.takeIf { it.isNotBlank() }?.let {
                                append("，").append(it)
                            }
                            session.teacher?.takeIf { it.isNotBlank() }?.let {
                                append("，").append(it)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSessionClick(session) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = description
                                    role = Role.Button
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (finished) {
                                            MaterialTheme.colorScheme.outline
                                        } else {
                                            courseColors(session.colorIndex).container
                                        },
                                    ),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.name,
                                    style = MaterialTheme.typography.bodyLarge,
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
                                Text(
                                    text = "${dayLabel(session.dayOfWeek)} " +
                                        periodLabel(session.startPeriod, session.periodCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (finished) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            session.room?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (finished) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 96.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoTimetableState(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("还没有课表", style = MaterialTheme.typography.titleMedium)
        Text(
            "创建一张课表后就可以添加课程了。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onCreate) { Text("新建课表") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimetablePickerDialog(
    state: ScheduleUiState,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择课表") },
        text = {
            Column {
                state.timetables.forEach { timetable ->
                    ListItem(
                        headlineContent = { Text(timetable.name) },
                        leadingContent = {
                            RadioButton(
                                selected = state.selectedTimetable?.id == timetable.id,
                                onClick = null,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        ),
                        modifier = Modifier.clickable { onSelect(timetable.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
internal fun CreateTimetableDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建课表") },
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
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

