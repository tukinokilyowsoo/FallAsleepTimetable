package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.CourseWithSessions
import java.time.DayOfWeek

/**
 * CSV 课程表交换格式。
 *
 * 导出列：课程名称,教师,教室,星期,开始节次,节数,开始周,结束周,单双周
 * 导入兼容列名别名、中文/数字/英文星期、1-16 这样的周次范围和引号字段。
 */
object CsvExporter {
    private val header = listOf(
        "课程名称", "教师", "教室", "星期",
        "开始节次", "节数", "开始周", "结束周", "单双周",
    )

    fun build(courses: List<CourseWithSessions>): String {
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.append(header.joinToString(",") { escape(it) }).append("\r\n")

        courses.forEach { entry ->
            entry.sessions.forEach { session ->
                val row = listOf(
                    entry.course.name,
                    entry.course.teacher.orEmpty(),
                    session.room.orEmpty(),
                    dayName(session.dayOfWeek),
                    session.startPeriod.toString(),
                    session.periodCount.toString(),
                    session.startWeek.toString(),
                    session.endWeek.toString(),
                    parityName(session.parity),
                )
                builder.append(row.joinToString(",") { escape(it) }).append("\r\n")
            }
        }
        return builder.toString()
    }

    private fun dayName(dayOfWeek: Int): String {
        val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names.getOrElse(dayOfWeek - 1) { "周一" }
    }

    private fun parityName(parity: Int): String = when (parity) {
        1 -> "单周"
        2 -> "双周"
        else -> "全周"
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}

object CsvImporter {
    private class Table(val rows: List<List<String>>)

    fun looksLikeCsv(text: String): Boolean {
        val first = text.removePrefix("\uFEFF")
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?: return false
        if (first.trimStart().startsWith("{") || first.trimStart().startsWith("[")) return false
        if (!first.contains(',') && !first.contains('\t')) return false
        return true
    }

    fun parse(text: String): ImportedTimetable {
        val table = readTable(text.removePrefix("\uFEFF"))
        require(table.rows.isNotEmpty()) { "文件是空的" }

        val header = table.rows.first().map { normalizeCsvHeader(it) }
        val columns = ColumnMap.from(header)
        require(columns.name != null) { "缺少「课程名称」列" }
        require(columns.day != null) { "缺少「星期」列" }
        require(columns.startPeriod != null) { "缺少「开始节次」列" }

        val courses = linkedMapOf<Pair<String, String?>, MutableList<ImportedSession>>()
        var skipped = 0

        table.rows.drop(1).forEach { row ->
            if (row.all { it.isBlank() }) return@forEach
            val name = columns.value(row, columns.name).trim()
            if (name.isBlank()) {
                skipped++
                return@forEach
            }
            val day = parseDay(columns.value(row, columns.day)) ?: run {
                skipped++
                return@forEach
            }
            val startPeriod = parseRange(columns.value(row, columns.startPeriod))?.first
                ?: columns.value(row, columns.startPeriod).toIntOrNull()
                ?: run {
                    skipped++
                    return@forEach
                }
            val periodCount = columns.value(row, columns.periodCount).toIntOrNull()
                ?: parseRange(columns.value(row, columns.startPeriod))?.let { it.second - it.first + 1 }
                ?: 1

            val weekRange = parseRange(columns.value(row, columns.startWeek))
            val startWeek = weekRange?.first
                ?: columns.value(row, columns.startWeek).toIntOrNull()
                ?: 1
            val endWeek = columns.value(row, columns.endWeek).toIntOrNull()
                ?: weekRange?.second
                ?: startWeek

            val teacher = columns.value(row, columns.teacher).trim().ifBlank { null }
            val room = columns.value(row, columns.room).trim().ifBlank { null }
            val parity = parseParity(columns.value(row, columns.parity))

            val session = ImportedSession(
                dayOfWeek = day,
                startPeriod = startPeriod.coerceAtLeast(1),
                periodCount = periodCount.coerceAtLeast(1),
                startWeek = startWeek.coerceAtLeast(1),
                endWeek = maxOf(startWeek, endWeek),
                parity = parity,
                room = room,
            )

            val key = name to teacher
            val sessions = courses.getOrPut(key) { mutableListOf() }
            if (session !in sessions) {
                sessions += session
            }
        }

        val imported = courses.map { (key, sessions) ->
            ImportedCourse(
                name = key.first,
                teacher = key.second,
                colorIndex = ((key.first.hashCode() % 6) + 6) % 6,
                sessions = sessions,
            )
        }
        require(imported.isNotEmpty()) {
            if (skipped > 0) {
                "没有可导入的课程；有 $skipped 行缺少课程名、星期或节次"
            } else {
                "没有可导入的课程"
            }
        }
        return ImportedTimetable(name = "导入的课表", courses = imported)
    }

    private class ColumnMap(
        val name: Int?,
        val teacher: Int?,
        val room: Int?,
        val day: Int?,
        val startPeriod: Int?,
        val periodCount: Int?,
        val startWeek: Int?,
        val endWeek: Int?,
        val parity: Int?,
    ) {
        fun value(row: List<String>, index: Int?): String =
            if (index == null || index !in row.indices) "" else row[index]

        companion object {
            fun from(header: List<String>): ColumnMap {
                fun find(vararg aliases: String): Int? {
                    val normalized = aliases.map(::normalizeCsvHeader)
                    return header.indexOfFirst { it in normalized }.takeIf { it >= 0 }
                }

                return ColumnMap(
                    name = find("课程名称", "课程", "课程名", "名称", "name", "course", "subject"),
                    teacher = find("教师", "老师", "任课教师", "teacher"),
                    room = find("教室", "地点", "上课地点", "room", "location"),
                    day = find("星期", "周几", "星期几", "day", "weekday", "dayofweek"),
                    startPeriod = find(
                        "开始节次", "节次", "起始节次", "第几节",
                        "startperiod", "period", "start",
                    ),
                    periodCount = find(
                        "节数", "连节", "节次数量", "上课节数",
                        "periodcount", "count", "periods", "length",
                    ),
                    startWeek = find(
                        "开始周", "起始周", "开始周次", "周次",
                        "startweek", "week", "start",
                    ),
                    endWeek = find("结束周", "终止周", "结束周次", "endweek", "end"),
                    parity = find("单双周", "周类型", "类型", "parity", "weektype"),
                )
            }
        }
    }

    private fun parseDay(value: String): Int? {
        val cleaned = value.trim()
            .lowercase()
            .replace("星期", "")
            .replace("礼拜", "")
            .replace("周", "")
            .trim()
        if (cleaned.isEmpty()) return null
        cleaned.toIntOrNull()?.let { return it.takeIf { day -> day in 1..7 } }
        return when (cleaned) {
            "一", "mon", "monday", "mo" -> 1
            "二", "tue", "tues", "tuesday", "tu" -> 2
            "三", "wed", "wednesday", "we" -> 3
            "四", "thu", "thur", "thurs", "thursday", "th" -> 4
            "五", "fri", "friday", "fr" -> 5
            "六", "sat", "saturday", "sa" -> 6
            "日", "天", "sun", "sunday", "su" -> 7
            else -> runCatching { DayOfWeek.valueOf(cleaned.uppercase()).value }.getOrNull()
        }
    }

    private fun parseParity(value: String): Int {
        val cleaned = value.trim().lowercase().replace(" ", "")
        return when {
            cleaned.isEmpty() -> 0
            cleaned in setOf("单", "单周", "odd", "1", "奇", "奇数") -> 1
            cleaned in setOf("双", "双周", "even", "2", "偶", "偶数") -> 2
            cleaned.contains("单") -> 1
            cleaned.contains("双") -> 2
            else -> 0
        }
    }

    private fun parseRange(value: String): Pair<Int, Int>? {
        val cleaned = value.trim()
        if (cleaned.isEmpty()) return null
        val match = Regex("(\\d+)\\s*[-~～—–至到]\\s*(\\d+)").find(cleaned) ?: return null
        val first = match.groupValues[1].toIntOrNull() ?: return null
        val last = match.groupValues[2].toIntOrNull() ?: return null
        if (first <= 0 || last < first) return null
        return first to last
    }

    private fun readTable(text: String): Table {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        fun endField() {
            row += field.toString()
            field.clear()
        }

        fun endRow() {
            endField()
            if (row.any { it.isNotBlank() }) rows += row.toList()
            row.clear()
        }

        while (index < text.length) {
            val ch = text[index]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (index + 1 < text.length && text[index + 1] == '"') {
                            field.append('"')
                            index++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(ch)
                    }
                }

                ch == '"' && field.isEmpty() -> inQuotes = true
                ch == ',' || ch == '\t' -> endField()
                ch == '\r' -> {
                    if (index + 1 < text.length && text[index + 1] == '\n') index++
                    endRow()
                }

                ch == '\n' -> endRow()
                else -> field.append(ch)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return Table(rows)
    }
}

private fun normalizeCsvHeader(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("[\\s_\\-（）()\\[\\]]"), "")
