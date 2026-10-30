package io.github.fallasleeptimetable.ui

private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")

fun dayLabel(dayOfWeek: Int): String =
    "周" + dayNames.getOrElse(dayOfWeek - 1) { "?" }

fun dayShortLabel(dayOfWeek: Int): String =
    dayNames.getOrElse(dayOfWeek - 1) { "?" }

fun periodLabel(startPeriod: Int, periodCount: Int): String =
    if (periodCount <= 1) {
        "第 $startPeriod 节"
    } else {
        "第 $startPeriod-${startPeriod + periodCount - 1} 节"
    }

fun weekRangeLabel(startWeek: Int, endWeek: Int, parity: Int): String {
    val base = if (startWeek == endWeek) "第 $startWeek 周" else "第 $startWeek-$endWeek 周"
    return when (parity) {
        1 -> "$base（单周）"
        2 -> "$base（双周）"
        else -> base
    }
}

fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)
