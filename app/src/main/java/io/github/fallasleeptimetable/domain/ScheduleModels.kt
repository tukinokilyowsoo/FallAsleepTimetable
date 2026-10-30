package io.github.fallasleeptimetable.domain

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
