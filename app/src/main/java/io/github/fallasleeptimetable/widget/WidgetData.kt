package io.github.fallasleeptimetable.widget

import android.content.Context
import io.github.fallasleeptimetable.data.AppDatabase
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.data.TimetableEntity
import io.github.fallasleeptimetable.domain.sessionActiveInWeek
import io.github.fallasleeptimetable.domain.weekNumberFor
import java.time.LocalDate

data class WidgetSession(
    val sessionId: Long,
    val name: String,
    val room: String?,
    val teacher: String?,
    val colorIndex: Int,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periodCount: Int,
    val startMinute: Int,
    val endMinute: Int,
)

data class WidgetData(
    val term: TermEntity,
    val timetable: TimetableEntity,
    val courses: List<CourseWithSessions>,
    val timeSlots: List<TimeSlotEntity>,
    val config: WidgetConfig = WidgetConfig(),
) {
    fun sessionsForWeek(week: Int): List<WidgetSession> = courses.flatMap { entry ->
        entry.sessions
            .filter { sessionActiveInWeek(it.startWeek, it.endWeek, it.parity, week) }
            .mapNotNull { session ->
                val startSlot = timeSlots.firstOrNull { it.period == session.startPeriod }
                val endPeriod = session.startPeriod + session.periodCount - 1
                val endSlot = timeSlots.firstOrNull { it.period == endPeriod }
                if (startSlot == null || endSlot == null) return@mapNotNull null
                WidgetSession(
                    sessionId = session.id,
                    name = entry.course.name,
                    room = session.room,
                    teacher = entry.course.teacher,
                    colorIndex = entry.course.colorIndex,
                    dayOfWeek = session.dayOfWeek,
                    startPeriod = session.startPeriod,
                    periodCount = session.periodCount,
                    startMinute = startSlot.startMinute,
                    endMinute = endSlot.endMinute,
                )
            }
    }

    fun actualWeek(today: LocalDate): Int =
        weekNumberFor(today, LocalDate.ofEpochDay(term.startDateEpochDay))
}

object WidgetDataLoader {
    /**
     * @param appWidgetId 小组件实例 ID；-1 表示使用应用当前课表（用于闹钟调度等场景）。
     */
    suspend fun load(context: Context, appWidgetId: Int = -1): WidgetData? {
        val db = AppDatabase.get(context)
        val term = db.termDao().getFirst() ?: return null
        val tables = db.timetableDao().getAll()
        if (tables.isEmpty()) return null

        val config = if (appWidgetId >= 0) {
            WidgetConfigStore.load(context, appWidgetId)
        } else {
            WidgetConfig()
        }

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selectedId = prefs.getLong("selected_timetable", -1L)
        val timetable = config.timetableId
            ?.let { id -> tables.firstOrNull { it.id == id } }
            ?: tables.firstOrNull { it.id == selectedId }
            ?: tables.first()

        return WidgetData(
            term = term,
            timetable = timetable,
            courses = db.courseDao().getWithSessions(timetable.id),
            timeSlots = db.timeSlotDao().getByTerm(term.id),
            config = config,
        )
    }
}

