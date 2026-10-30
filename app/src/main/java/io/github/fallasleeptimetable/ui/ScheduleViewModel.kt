package io.github.fallasleeptimetable.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.fallasleeptimetable.BuildConfig
import io.github.fallasleeptimetable.data.AppContainer
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.data.TimetableEntity
import io.github.fallasleeptimetable.data.importer.TimetableExporter
import io.github.fallasleeptimetable.domain.CourseDraft
import io.github.fallasleeptimetable.domain.weekNumberFor
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

    fun consumeMessage() {
        _message.value = null
    }

    fun exportTimetableJson(): String? {
        val state = uiState.value
        val table = state.selectedTimetable ?: return null
        val term = state.term ?: return null
        return TimetableExporter.buildJson(term, table, state.courses, state.timeSlots)
    }

    fun importTimetableText(text: String) {
        val term = uiState.value.term
        if (term == null) {
            _message.value = "还没有学期，无法导入"
            return
        }
        viewModelScope.launch {
            runCatching { repository.importTimetableFromText(term.id, text) }
                .onSuccess { result ->
                    selectTimetable(result.timetableId)
                    _message.value = "已导入 ${result.courseCount} 门课程"
                }
                .onFailure { error ->
                    _message.value = "导入失败：${error.message ?: "文件格式不支持"}"
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
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch { repository.deleteCourse(courseId) }
    }

    fun createTimetable(name: String) {
        val term = uiState.value.term ?: return
        viewModelScope.launch {
            val id = repository.createTimetable(term.id, name)
            selectTimetable(id)
        }
    }

    fun renameTimetable(id: Long, name: String) {
        viewModelScope.launch { repository.renameTimetable(id, name) }
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
        }
    }

    fun updateTerm(term: TermEntity) {
        viewModelScope.launch {
            repository.updateTerm(term)
            weekOverride.value = null
        }
    }

    fun updateTimeSlots(periodsPerDay: Int, slots: List<Pair<Int, Int>>) {
        val term = uiState.value.term ?: return
        viewModelScope.launch {
            repository.replaceTimeSlots(term.id, periodsPerDay, slots)
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
