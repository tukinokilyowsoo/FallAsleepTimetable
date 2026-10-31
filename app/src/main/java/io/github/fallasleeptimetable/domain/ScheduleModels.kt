package io.github.fallasleeptimetable.domain

import io.github.fallasleeptimetable.data.CourseWithSessions
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CourseDraft(
    val name: String,
    val teacher: String?,
    val room: String?,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periodCount: Int,
    val startWeek: Int,
    val endWeek: Int,
    val parity: Int,
    val colorIndex: Int,
)

data class VisibleSession(
    val courseId: Long,
    val sessionId: Long,
    val name: String,
    val teacher: String?,
    val room: String?,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periodCount: Int,
    val startWeek: Int,
    val endWeek: Int,
    val parity: Int,
    val colorIndex: Int,
)

fun weekNumberFor(date: LocalDate, startDate: LocalDate): Int {
    val days = ChronoUnit.DAYS.between(startDate, date)
    return (Math.floorDiv(days, 7L) + 1).toInt()
}

fun sessionActiveInWeek(startWeek: Int, endWeek: Int, parity: Int, week: Int): Boolean {
    if (week < startWeek || week > endWeek) return false
    return when (parity) {
        1 -> week % 2 == 1
        2 -> week % 2 == 0
        else -> true
    }
}

/** 某一周实际要显示的课程安排。 */
fun visibleSessions(courses: List<CourseWithSessions>, week: Int): List<VisibleSession> =
    courses.flatMap { entry ->
        entry.sessions
            .filter { session ->
                sessionActiveInWeek(session.startWeek, session.endWeek, session.parity, week)
            }
            .map { session ->
                VisibleSession(
                    courseId = entry.course.id,
                    sessionId = session.id,
                    name = entry.course.name,
                    teacher = entry.course.teacher,
                    room = session.room,
                    dayOfWeek = session.dayOfWeek,
                    startPeriod = session.startPeriod,
                    periodCount = session.periodCount,
                    startWeek = session.startWeek,
                    endWeek = session.endWeek,
                    parity = session.parity,
                    colorIndex = entry.course.colorIndex,
                )
            }
    }

/** 同一天课程重叠时分栏；返回每节课所在栏与总栏数。 */
fun assignLanes(sessions: List<VisibleSession>): Pair<Map<Long, Int>, Int> {
    val laneEnds = mutableListOf<Int>()
    val laneOf = mutableMapOf<Long, Int>()
    sessions
        .sortedWith(compareBy({ it.startPeriod }, { it.sessionId }))
        .forEach { session ->
            val end = session.startPeriod + session.periodCount - 1
            val free = laneEnds.indexOfFirst { it < session.startPeriod }
            if (free == -1) {
                laneOf[session.sessionId] = laneEnds.size
                laneEnds.add(end)
            } else {
                laneOf[session.sessionId] = free
                laneEnds[free] = end
            }
        }
    return laneOf to laneEnds.size.coerceAtLeast(1)
}
