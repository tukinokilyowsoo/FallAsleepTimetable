package io.github.fallasleeptimetable.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.fallasleeptimetable.BuildConfig
import io.github.fallasleeptimetable.data.AppContainer
import io.github.fallasleeptimetable.data.CourseSearchRow
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.data.TimetableEntity
import io.github.fallasleeptimetable.data.importer.CsvExporter
import io.github.fallasleeptimetable.data.importer.IcsExporter
import io.github.fallasleeptimetable.data.importer.TimetableExporter
import io.github.fallasleeptimetable.domain.CourseDraft
import io.github.fallasleeptimetable.domain.sessionActiveInWeek
import io.github.fallasleeptimetable.domain.weekNumberFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import io.github.fallasleeptimetable.widget.WidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleUiState(
    val ready: Boolean = false,
    val term: TermEntity? = null,
    val timetables: List<TimetableEntity> = emptyList(),
    val selectedTimetable: TimetableEntity? = null,
    val courses: List<CourseWithSessions> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val currentWeek: Int = 1,
    val actualWeek: Int = 1,
    val isCurrentWeek: Boolean = true,
    val weekStart: LocalDate? = null,
    val todayDayOfWeek: Int = 1,
)

private data class BaseState(
    val term: TermEntity?,
    val tables: List<TimetableEntity>,
    val table: TimetableEntity?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppContainer.repository(application)
    private val appContext = application.applicationContext
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val selectedId = MutableStateFlow(
        prefs.getLong(KEY_SELECTED_TIMETABLE, -1L).takeIf { it > 0 },
    )
    private val weekOverride = MutableStateFlow<Int?>(null)

    private val base = combine(
        repository.term,
        repository.timetables,
        selectedId,
    ) { term, tables, selected ->
        BaseState(
            term = term,
            tables = tables,
            table = tables.firstOrNull { it.id == selected } ?: tables.firstOrNull(),
        )
    }

    val uiState: StateFlow<ScheduleUiState> = base.flatMapLatest { baseState ->
        val term = baseState.term
        val table = baseState.table
        when {
            term == null -> flowOf(ScheduleUiState())
            table == null -> flowOf(
                ScheduleUiState(
                    ready = true,
                    term = term,
                    timetables = baseState.tables,
                ),
            )

            else -> combine(
                repository.courses(table.id),
                repository.timeSlots(term.id),
                weekOverride,
            ) { courses, slots, override ->
                buildState(baseState, courses, slots, override)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleUiState(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<CourseSearchRow>>(emptyList())
    val searchResults: StateFlow<List<CourseSearchRow>> = _searchResults

    private var searchJob: Job? = null

    fun consumeMessage() {
        _message.value = null
    }

    fun searchCourses(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(150)
            _searchResults.value = runCatching {
                repository.searchCourses(query)
            }.getOrDefault(emptyList())
        }
    }

    fun jumpToSearchResult(result: CourseSearchRow) {
        selectTimetable(result.timetableId)
        val term = uiState.value.term ?: return
        val actualWeek = uiState.value.actualWeek
        val week = if (
            sessionActiveInWeek(
                result.startWeek,
                result.endWeek,
                result.parity,
                actualWeek,
            )
        ) {
            actualWeek
        } else {
            (result.startWeek..result.endWeek).firstOrNull { week ->
                sessionActiveInWeek(result.startWeek, result.endWeek, result.parity, week)
            } ?: result.startWeek
        }
        weekOverride.value = week.coerceIn(1, term.totalWeeks)
    }

    fun exportTimetableJson(): String? {
        val state = uiState.value
        val table = state.selectedTimetable ?: return null
        val term = state.term ?: return null
        return TimetableExporter.buildJson(term, table, state.courses, state.timeSlots)
    }

    fun exportTimetableIcs(): String? {
        val state = uiState.value
        val table = state.selectedTimetable ?: return null
        val term = state.term ?: return null
        if (state.courses.isEmpty()) return null
        return IcsExporter.build(
            term = term,
            timetableName = table.name,
            courses = state.courses,
            timeSlots = state.timeSlots,
        )
    }

    fun exportTimetableCsv(): String? {
        val state = uiState.value
        if (state.selectedTimetable == null || state.courses.isEmpty()) return null
        return CsvExporter.build(state.courses)
    }

    fun importTimetableText(
        text: String,
        applySettings: Boolean = false,
        onResult: ((String) -> Unit)? = null,
    ) {
        fun report(message: String) {
            if (onResult != null) {
                onResult(message)
            } else {
                _message.value = message
            }
        }

        val term = uiState.value.term
        if (term == null) {
            report("还没有学期，无法导入")
            return
        }
        viewModelScope.launch {
            runCatching { repository.importTimetableFromText(term.id, text, applySettings) }
                .onSuccess { result ->
                    selectTimetable(result.timetableId)
                    weekOverride.value = null
                    report(
                        buildString {
                            append("已导入 ").append(result.courseCount).append(" 门课程")
                            if (applySettings) append("，并恢复学期设置与节次时间")
                        },
                    )
                    WidgetUpdater.updateAll(appContext)
                }
                .onFailure { error ->
                    report("导入失败：${error.message ?: "文件格式不支持"}")
                }
        }
    }

    init {
        viewModelScope.launch {
            repository.ensureSeed(debugSample = BuildConfig.DEBUG)
            val tables = repository.timetables.first()
            if (selectedId.value == null || tables.none { it.id == selectedId.value }) {
                selectedId.value = tables.firstOrNull()?.id
            }
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun selectTimetable(id: Long) {
        selectedId.value = id
        prefs.edit().putLong(KEY_SELECTED_TIMETABLE, id).apply()
    }

    fun previousWeek() {
        val state = uiState.value
        weekOverride.value = (state.currentWeek - 1).coerceAtLeast(1)
    }

    fun nextWeek() {
        val state = uiState.value
        val max = state.term?.totalWeeks ?: state.currentWeek
        weekOverride.value = (state.currentWeek + 1).coerceAtMost(max)
    }

    fun jumpToCurrentWeek() {
        weekOverride.value = null
    }

    fun saveCourse(courseId: Long?, sessionId: Long?, draft: CourseDraft) {
        val table = uiState.value.selectedTimetable ?: return
        viewModelScope.launch {
            repository.saveCourse(table.id, courseId, sessionId, draft)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun createTimetable(name: String) {
        val term = uiState.value.term ?: return
        viewModelScope.launch {
            val id = repository.createTimetable(term.id, name)
            selectTimetable(id)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun renameTimetable(id: Long, name: String) {
        viewModelScope.launch {
            repository.renameTimetable(id, name)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun deleteTimetable(id: Long) {
        viewModelScope.launch {
            repository.deleteTimetable(id)
            val remaining = repository.timetables.first()
            val next = remaining.firstOrNull()?.id
            selectedId.value = next
            if (next != null) {
                prefs.edit().putLong(KEY_SELECTED_TIMETABLE, next).apply()
            } else {
                prefs.edit().remove(KEY_SELECTED_TIMETABLE).apply()
            }
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun updateTerm(term: TermEntity) {
        viewModelScope.launch {
            repository.updateTerm(term)
            weekOverride.value = null
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun updateTimeSlots(periodsPerDay: Int, slots: List<Pair<Int, Int>>) {
        val term = uiState.value.term ?: return
        viewModelScope.launch {
            repository.replaceTimeSlots(term.id, periodsPerDay, slots)
            WidgetUpdater.updateAll(appContext)
        }
    }

    private fun buildState(
        baseState: BaseState,
        courses: List<CourseWithSessions>,
        slots: List<TimeSlotEntity>,
        override: Int?,
    ): ScheduleUiState {
        val term = requireNotNull(baseState.term)
        val start = LocalDate.ofEpochDay(term.startDateEpochDay)
        val today = LocalDate.now()
        val actualWeek = weekNumberFor(today, start).coerceIn(1, term.totalWeeks)
        val week = (override ?: actualWeek).coerceIn(1, term.totalWeeks)
        return ScheduleUiState(
            ready = true,
            term = term,
            timetables = baseState.tables,
            selectedTimetable = baseState.table,
            courses = courses,
            timeSlots = slots,
            currentWeek = week,
            actualWeek = actualWeek,
            isCurrentWeek = week == actualWeek,
            weekStart = start.plusWeeks((week - 1).toLong()),
            todayDayOfWeek = today.dayOfWeek.value,
        )
    }

    private companion object {
        const val KEY_SELECTED_TIMETABLE = "selected_timetable"
    }
}
