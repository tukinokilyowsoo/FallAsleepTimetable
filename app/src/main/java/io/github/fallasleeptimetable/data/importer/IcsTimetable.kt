package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.CourseSessionEntity
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.domain.weekNumberFor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * ICS（RFC 5545）导出与导入。
 *
 * 导出：每次上课安排生成一个 VEVENT，用 RRULE 表达连续周与单双周。
 * 时间使用浮动时间（不带 TZID），即设备本地墙钟时间，换时区上课时间仍按当地时间。
 * 额外的 X-FALLASLEEP-* 属性用于本应用精确往返；其他日历会忽略它们。
 */
object IcsExporter {
    private val stampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun build(
        term: TermEntity,
        timetableName: String,
        courses: List<CourseWithSessions>,
        timeSlots: List<TimeSlotEntity>,
        now: Instant = Instant.now(),
    ): String {
        val startDate = LocalDate.ofEpochDay(term.startDateEpochDay)
        val slotByPeriod = timeSlots.associateBy { it.period }
        val stamp = now.atZone(ZoneOffset.UTC).format(stampFormatter)

        val lines = mutableListOf<String>()
        lines += "BEGIN:VCALENDAR"
        lines += "VERSION:2.0"
        lines += "PRODID:-//FallAsleep//Timetable//CN"
        lines += "CALSCALE:GREGORIAN"
        lines += "METHOD:PUBLISH"
        lines += property("X-WR-CALNAME", timetableName.trim().ifBlank { "课程表" })

        courses.forEach { entry ->
            entry.sessions.forEach sessionLoop@{ session ->
                val activeWeeks = activeWeeks(session)
                if (activeWeeks.isEmpty()) return@sessionLoop

                val startSlot = slotByPeriod[session.startPeriod] ?: return@sessionLoop
                val endSlot = slotByPeriod[session.startPeriod + session.periodCount - 1]
                    ?: return@sessionLoop
                if (endSlot.endMinute <= startSlot.startMinute) return@sessionLoop
                if (session.dayOfWeek !in 1..7) return@sessionLoop

                val firstWeek = activeWeeks.first()
                val date = startDate
                    .plusWeeks((firstWeek - 1).toLong())
                    .plusDays((session.dayOfWeek - 1).toLong())
                val start = LocalDateTime.of(
                    date.year,
                    date.month,
                    date.dayOfMonth,
                    startSlot.startMinute / 60,
                    startSlot.startMinute % 60,
                )
                val end = LocalDateTime.of(
                    date.year,
                    date.month,
                    date.dayOfMonth,
                    endSlot.endMinute / 60,
                    endSlot.endMinute % 60,
                )

                lines += "BEGIN:VEVENT"
                lines += "UID:fallasleep-${term.id}-${entry.course.id}-${session.id}@fallasleep.app"
                lines += "DTSTAMP:$stamp"
                lines += "DTSTART:${start.format(dateTimeFormatter)}"
                lines += "DTEND:${end.format(dateTimeFormatter)}"
                lines += property("SUMMARY", entry.course.name)
                session.room?.takeIf { it.isNotBlank() }?.let {
                    lines += property("LOCATION", it)
                }
                lines += property("DESCRIPTION", description(entry, session))
                lines += "RRULE:FREQ=WEEKLY;INTERVAL=${if (session.parity == 0) 1 else 2};" +
                    "COUNT=${activeWeeks.size};WKST=MO"
                lines += "X-FALLASLEEP-START-WEEK:${session.startWeek}"
                lines += "X-FALLASLEEP-END-WEEK:${session.endWeek}"
                lines += "X-FALLASLEEP-PARITY:${session.parity}"
                lines += "X-FALLASLEEP-START-PERIOD:${session.startPeriod}"
                lines += "X-FALLASLEEP-PERIOD-COUNT:${session.periodCount}"
                entry.course.teacher?.takeIf { it.isNotBlank() }?.let {
                    lines += property("X-FALLASLEEP-TEACHER", it)
                }
                lines += "END:VEVENT"
            }
        }

        lines += "END:VCALENDAR"
        return lines.joinToString("\r\n", postfix = "\r\n") { foldLine(it) }
    }

    private fun description(
        entry: CourseWithSessions,
        session: CourseSessionEntity,
    ): String = buildString {
        entry.course.teacher?.takeIf { it.isNotBlank() }?.let {
            append("教师：").append(it).append('\n')
        }
        append("节次：第 ").append(session.startPeriod)
        if (session.periodCount > 1) {
            append('-').append(session.startPeriod + session.periodCount - 1)
        }
        append(" 节\n")
        append("周次：").append(weekText(session.startWeek, session.endWeek, session.parity))
        append("\n由 FallAsleep 课程表导出")
    }

    private fun activeWeeks(session: CourseSessionEntity): List<Int> {
        val range = session.startWeek..session.endWeek
        return when (session.parity) {
            1 -> range.filter { it % 2 == 1 }
            2 -> range.filter { it % 2 == 0 }
            else -> range.toList()
        }
    }

    private fun weekText(startWeek: Int, endWeek: Int, parity: Int): String {
        val base = if (startWeek == endWeek) {
            "第 $startWeek 周"
        } else {
            "第 $startWeek-$endWeek 周"
        }
        return when (parity) {
            1 -> "$base（单周）"
            2 -> "$base（双周）"
            else -> base
        }
    }

    private fun property(name: String, value: String): String = "$name:${escapeText(value)}"

    private fun escapeText(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(ch)
            }
        }
    }

    /** RFC 5545 规定每行不超过 75 字节；折行时不能拆开 UTF-8 字符。 */
    private fun foldLine(line: String): String {
        if (line.toByteArray(Charsets.UTF_8).size <= 75) return line
        val builder = StringBuilder()
        var bytes = 0
        var index = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val size = utf8Size(codePoint)
            if (bytes + size > 73) {
                builder.append("\r\n ")
                bytes = 0
            }
            builder.appendCodePoint(codePoint)
            bytes += size
            index += Character.charCount(codePoint)
        }
        return builder.toString()
    }

    private fun utf8Size(codePoint: Int): Int = when {
        codePoint <= 0x7F -> 1
        codePoint <= 0x7FF -> 2
        codePoint <= 0xFFFF -> 3
        else -> 4
    }
}

object IcsImporter {
    private const val UNTIL = "UNTIL"

    private data class VEvent(
        val properties: Map<String, String>,
        val params: Map<String, Map<String, String>>,
    )

    fun parse(
        text: String,
        term: TermEntity,
        timeSlots: List<TimeSlotEntity>,
        fallbackName: String = "导入的课表",
        systemZone: ZoneId = ZoneId.systemDefault(),
    ): ImportedTimetable {
        val content = text.removePrefix("\uFEFF")
        require(content.contains("BEGIN:VEVENT", ignoreCase = true)) {
            "文件里没有可导入的日程"
        }

        val unfolded = content
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\n[ \t]"), "")

        val globalProperties = mutableMapOf<String, String>()
        val events = mutableListOf<VEvent>()
        var current: MutableMap<String, String>? = null
        var currentParams: MutableMap<String, Map<String, String>>? = null

        unfolded.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    current = linkedMapOf()
                    currentParams = linkedMapOf()
                }

                line.equals("END:VEVENT", ignoreCase = true) -> {
                    val properties = current
                    if (properties != null) {
                        events += VEvent(properties, currentParams.orEmpty())
                    }
                    current = null
                    currentParams = null
                }

                current != null -> {
                    val (head, value) = splitProperty(line) ?: return@forEach
                    val (name, params) = splitPropertyHead(head)
                    current!![name] = value
                    if (params.isNotEmpty()) {
                        currentParams?.put(name, params)
                    }
                }

                else -> {
                    val (head, value) = splitProperty(line) ?: return@forEach
                    val (name, _) = splitPropertyHead(head)
                    globalProperties[name] = value
                }
            }
        }

        val termStart = LocalDate.ofEpochDay(term.startDateEpochDay)
        val calendarName = globalProperties["X-WR-CALNAME"]
            ?.let(::unescape)
            ?.takeIf { it.isNotBlank() }
            ?: fallbackName

        val courses = linkedMapOf<Pair<String, String?>, MutableList<ImportedSession>>()

        events.forEach { event ->
            if (event.properties.containsKey("RECURRENCE-ID")) return@forEach

            val summary = event.properties["SUMMARY"]?.let(::unescape).orEmpty()
                .trim()
                .ifBlank { "未命名课程" }

            val startRaw = event.properties["DTSTART"] ?: return@forEach
            val start = parseDateTime(startRaw, event.params["DTSTART"].orEmpty(), systemZone)
                ?: return@forEach
            val startDate = start.toLocalDate()
            val startWeek = weekNumberFor(startDate, termStart)
            if (startWeek !in 1..term.totalWeeks) return@forEach

            val startSlot = closestSlot(
                timeSlots,
                start.hour * 60 + start.minute,
                useEnd = false,
            ) ?: return@forEach

            val endRaw = event.properties["DTEND"]
            val endPeriod = if (endRaw != null) {
                val end = parseDateTime(
                    endRaw,
                    event.params["DTEND"].orEmpty(),
                    systemZone,
                )
                end?.let {
                    closestSlot(timeSlots, it.hour * 60 + it.minute, useEnd = true)
                }
            } else {
                null
            }

            val exactStartPeriod = event.properties["X-FALLASLEEP-START-PERIOD"]?.toIntOrNull()
            val exactPeriodCount = event.properties["X-FALLASLEEP-PERIOD-COUNT"]?.toIntOrNull()
            val startPeriod = exactStartPeriod ?: startSlot.period
            val periodCount = exactPeriodCount
                ?: ((endPeriod?.period ?: startPeriod) - startPeriod + 1).coerceAtLeast(1)

            val recurrence = parseRecurrence(event.properties["RRULE"].orEmpty())
            val exactStartWeek = event.properties["X-FALLASLEEP-START-WEEK"]?.toIntOrNull()
            val exactEndWeek = event.properties["X-FALLASLEEP-END-WEEK"]?.toIntOrNull()
            val exactParity = event.properties["X-FALLASLEEP-PARITY"]?.toIntOrNull()

            val parity = exactParity ?: when (recurrence.interval) {
                2 -> if (startWeek % 2 == 1) 1 else 2
                else -> 0
            }
            val actualStartWeek = (exactStartWeek ?: startWeek).coerceAtLeast(1)
            val actualEndWeek = when {
                exactEndWeek != null -> exactEndWeek
                recurrence.count != null ->
                    actualStartWeek + (recurrence.count - 1) * recurrence.interval

                recurrence.until != null ->
                    weekNumberFor(recurrence.until, termStart).coerceAtLeast(actualStartWeek)

                recurrence.interval > 0 && recurrence.hasRule -> term.totalWeeks
                else -> actualStartWeek
            }.coerceIn(actualStartWeek, maxOf(actualStartWeek, term.totalWeeks))

            if (actualStartWeek > term.totalWeeks) return@forEach

            val teacher = event.properties["X-FALLASLEEP-TEACHER"]?.let(::unescape)
                ?.takeIf { it.isNotBlank() }
                ?: teacherFromDescription(event.properties["DESCRIPTION"])
            val room = event.properties["LOCATION"]?.let(::unescape)?.takeIf { it.isNotBlank() }

            val session = ImportedSession(
                dayOfWeek = startDate.dayOfWeek.value.coerceIn(1, 7),
                startPeriod = startPeriod.coerceAtLeast(1),
                periodCount = periodCount.coerceIn(1, 20),
                startWeek = actualStartWeek,
                endWeek = actualEndWeek.coerceAtLeast(actualStartWeek),
                parity = parity.coerceIn(0, 2),
                room = room,
            )

            val key = summary to teacher
            val sessions = courses.getOrPut(key) { mutableListOf() }
            if (session !in sessions) {
                sessions += session
            }
        }

        val importedCourses = courses.map { (key, sessions) ->
            ImportedCourse(
                name = key.first,
                teacher = key.second,
                colorIndex = ((key.first.hashCode() % 6) + 6) % 6,
                sessions = mergeAdjacentWeeks(sessions),
            )
        }.filter { it.sessions.isNotEmpty() }

        require(importedCourses.isNotEmpty()) {
            "没有解析到课程；请确认事件有具体时间，且日期落在当前学期内"
        }

        return ImportedTimetable(name = calendarName, courses = importedCourses)
    }

    /** 有的日历会给每次课生成单独事件；把相邻的全周事件合并成一段。 */
    private fun mergeAdjacentWeeks(sessions: List<ImportedSession>): List<ImportedSession> {
        val sorted = sessions.sortedWith(
            compareBy(
                { it.dayOfWeek },
                { it.startPeriod },
                { it.startWeek },
                { it.room.orEmpty() },
            ),
        )
        val merged = mutableListOf<ImportedSession>()
        sorted.forEach { session ->
            val last = merged.lastOrNull()
            if (last != null &&
                last.parity == 0 &&
                session.parity == 0 &&
                last.dayOfWeek == session.dayOfWeek &&
                last.startPeriod == session.startPeriod &&
                last.periodCount == session.periodCount &&
                last.room == session.room &&
                session.startWeek == last.endWeek + 1
            ) {
                merged[merged.lastIndex] = last.copy(endWeek = session.endWeek)
            } else {
                merged += session
            }
        }
        return merged
    }

    private data class Recurrence(
        val interval: Int,
        val count: Int?,
        val until: LocalDate?,
        val hasRule: Boolean,
    )

    private fun parseRecurrence(value: String): Recurrence {
        if (value.isBlank()) return Recurrence(1, null, null, false)
        val parts = value.split(';')
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) null else part.substring(0, index).uppercase() to
                    part.substring(index + 1)
            }
            .toMap()
        val interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val count = parts["COUNT"]?.toIntOrNull()?.takeIf { it > 0 }
        val until = parts[UNTIL]?.let { parseDateOnly(it) }
        return Recurrence(interval, count, until, true)
    }

    private fun parseDateOnly(value: String): LocalDate? {
        val cleaned = value.trim().removeSuffix("Z")
        val datePart = cleaned.substringBefore('T')
        return runCatching {
            LocalDate.of(
                datePart.substring(0, 4).toInt(),
                datePart.substring(4, 6).toInt(),
                datePart.substring(6, 8).toInt(),
            )
        }.getOrNull()
    }

    private fun parseDateTime(
        raw: String,
        params: Map<String, String>,
        systemZone: ZoneId,
    ): ZonedDateTime? {
        val value = raw.trim()
        if (params["VALUE"].equals("DATE", ignoreCase = true)) return null
        if (!value.contains('T')) return null

        val utc = value.endsWith("Z", ignoreCase = true)
        val local = parseLocalDateTime(value.removeSuffix("Z").removeSuffix("z")) ?: return null
        val tzid = params["TZID"]?.trim('"', ' ')
        return when {
            utc -> local.atZone(ZoneOffset.UTC).withZoneSameInstant(systemZone)
            !tzid.isNullOrBlank() -> {
                val zone = runCatching { ZoneId.of(tzid) }.getOrDefault(systemZone)
                local.atZone(zone).withZoneSameInstant(systemZone)
            }

            else -> local.atZone(systemZone)
        }
    }

    private fun parseLocalDateTime(value: String): LocalDateTime? {
        val cleaned = value.trim()
        val datePart = cleaned.substringBefore('T')
        val timePart = cleaned.substringAfter('T', "")
        if (datePart.length != 8) return null
        val normalizedTime = timePart.padEnd(6, '0').take(6)
        if (!normalizedTime.all { it.isDigit() }) return null
        return runCatching {
            LocalDateTime.of(
                datePart.substring(0, 4).toInt(),
                datePart.substring(4, 6).toInt(),
                datePart.substring(6, 8).toInt(),
                normalizedTime.substring(0, 2).toInt(),
                normalizedTime.substring(2, 4).toInt(),
                normalizedTime.substring(4, 6).toInt(),
            )
        }.getOrNull()
    }

    private fun closestSlot(
        slots: List<TimeSlotEntity>,
        minute: Int,
        useEnd: Boolean,
    ): TimeSlotEntity? = slots
        .minByOrNull { slot ->
            abs((if (useEnd) slot.endMinute else slot.startMinute) - minute)
        }
        ?.takeIf { slot ->
            abs((if (useEnd) slot.endMinute else slot.startMinute) - minute) <= 45
        }

    private fun splitProperty(line: String): Pair<String, String>? {
        var inQuotes = false
        line.forEachIndexed { index, ch ->
            if (ch == '"') inQuotes = !inQuotes
            if (ch == ':' && !inQuotes) {
                return line.substring(0, index) to line.substring(index + 1)
            }
        }
        return null
    }

    private fun splitPropertyHead(head: String): Pair<String, Map<String, String>> {
        val parts = mutableListOf<String>()
        val builder = StringBuilder()
        var inQuotes = false
        head.forEach { ch ->
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    builder.append(ch)
                }

                ch == ';' && !inQuotes -> {
                    parts += builder.toString()
                    builder.clear()
                }

                else -> builder.append(ch)
            }
        }
        parts += builder.toString()

        val name = parts.first().trim().uppercase()
        val params = parts.drop(1).mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else {
                part.substring(0, index).trim().uppercase() to
                    part.substring(index + 1).trim().trim('"')
            }
        }.toMap()
        return name to params
    }

    private fun unescape(value: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch == '\\' && index + 1 < value.length) {
                when (value[index + 1]) {
                    'n', 'N' -> builder.append('\n')
                    '\\' -> builder.append('\\')
                    ';' -> builder.append(';')
                    ',' -> builder.append(',')
                    else -> builder.append(value[index + 1])
                }
                index += 2
            } else {
                builder.append(ch)
                index++
            }
        }
        return builder.toString().trim()
    }

    private fun teacherFromDescription(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val description = unescape(raw)
        return Regex("(?:教师|老师)[：:]\\s*([^\\n]+)")
            .find(description)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
