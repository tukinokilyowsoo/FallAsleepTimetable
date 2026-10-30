package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.data.TimetableEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.absoluteValue

data class ImportedTimetable(
    val name: String,
    val courses: List<ImportedCourse>,
)

data class ImportedCourse(
    val name: String,
    val teacher: String?,
    val colorIndex: Int,
    val sessions: List<ImportedSession>,
)

data class ImportedSession(
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periodCount: Int,
    val startWeek: Int,
    val endWeek: Int,
    val parity: Int,
    val room: String?,
)

object TimetableExporter {
    fun buildJson(
        term: TermEntity,
        timetable: TimetableEntity,
        courses: List<CourseWithSessions>,
        timeSlots: List<TimeSlotEntity>,
    ): String {
        val root = JSONObject()
        root.put("app", "FallAsleep")
        root.put("version", 1)
        root.put("exportedAt", Instant.now().toString())
        root.put(
            "term",
            JSONObject().apply {
                put("name", term.name)
                put("startDateEpochDay", term.startDateEpochDay)
                put("totalWeeks", term.totalWeeks)
                put("periodsPerDay", term.periodsPerDay)
                put("showSaturday", term.showSaturday)
                put("showSunday", term.showSunday)
            },
        )
        root.put("timetable", JSONObject().put("name", timetable.name))
        root.put(
            "courses",
            JSONArray().apply {
                courses.forEach { entry ->
                    put(
                        JSONObject().apply {
                            put("name", entry.course.name)
                            put("teacher", entry.course.teacher ?: JSONObject.NULL)
                            put("colorIndex", entry.course.colorIndex)
                            put(
                                "sessions",
                                JSONArray().apply {
                                    entry.sessions.forEach { session ->
                                        put(
                                            JSONObject().apply {
                                                put("dayOfWeek", session.dayOfWeek)
                                                put("startPeriod", session.startPeriod)
                                                put("periodCount", session.periodCount)
                                                put("startWeek", session.startWeek)
                                                put("endWeek", session.endWeek)
                                                put("parity", session.parity)
                                                put("room", session.room ?: JSONObject.NULL)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
        root.put(
            "timeSlots",
            JSONArray().apply {
                timeSlots.forEach { slot ->
                    put(
                        JSONObject().apply {
                            put("period", slot.period)
                            put("startMinute", slot.startMinute)
                            put("endMinute", slot.endMinute)
                        },
                    )
                }
            },
        )
        return root.toString()
    }
}

object TimetableImporter {
    fun parse(text: String): ImportedTimetable {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "文件是空的" }

        val backup = runCatching { JSONObject(trimmed) }.getOrNull()
        if (backup != null && backup.has("courses")) {
            return parseBackup(backup)
        }
        return parseLegacy(trimmed)
    }

    private fun parseBackup(root: JSONObject): ImportedTimetable {
        val name = root.optJSONObject("timetable")
            ?.optString("name")
            ?.takeIf { it.isNotBlank() }
            ?: "导入的课表"

        val courses = mutableListOf<ImportedCourse>()
        val array = root.optJSONArray("courses") ?: JSONArray()
        for (index in 0 until array.length()) {
            val courseJson = array.optJSONObject(index) ?: continue
            val courseName = courseJson.optString("name").trim().ifBlank { "未命名课程" }
            val teacher = courseJson.optString("teacher").takeIf { it.isNotBlank() }
            val colorIndex = courseJson.optInt("colorIndex", index % 6)

            val sessions = mutableListOf<ImportedSession>()
            val sessionArray = courseJson.optJSONArray("sessions") ?: JSONArray()
            for (sessionIndex in 0 until sessionArray.length()) {
                val json = sessionArray.optJSONObject(sessionIndex) ?: continue
                sessions.add(
                    ImportedSession(
                        dayOfWeek = json.optInt("dayOfWeek", 1).coerceIn(1, 7),
                        startPeriod = json.optInt("startPeriod", 1).coerceAtLeast(1),
                        periodCount = json.optInt("periodCount", 1).coerceAtLeast(1),
                        startWeek = json.optInt("startWeek", 1).coerceAtLeast(1),
                        endWeek = json.optInt("endWeek", 20).coerceAtLeast(1),
                        parity = json.optInt("parity", 0).coerceIn(0, 2),
                        room = json.optString("room").takeIf { it.isNotBlank() },
                    ),
                )
            }
            if (sessions.isNotEmpty()) {
                courses.add(
                    ImportedCourse(
                        name = courseName,
                        teacher = teacher,
                        colorIndex = normalizeColor(colorIndex),
                        sessions = sessions,
                    ),
                )
            }
        }
        return ImportedTimetable(name = name, courses = courses)
    }

    /**
     * 旧版 WakeUp 课程表的 .wakeup_schedule：5 行 JSON，依次是
     * 时间表 / 节次时间 / 课表 / 课程基础 / 课程安排。
     */
    private fun parseLegacy(text: String): ImportedTimetable {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.size >= 5) { "无法识别的文件格式" }

        val tableJson = JSONObject(lines[2])
        val name = tableJson.optString("tableName").takeIf { it.isNotBlank() } ?: "导入的课表"

        val bases = runCatching { JSONArray(lines[3]) }.getOrElse { JSONArray() }
        val details = runCatching { JSONArray(lines[4]) }.getOrElse { JSONArray() }

        val detailsByCourse = mutableMapOf<Int, MutableList<JSONObject>>()
        for (index in 0 until details.length()) {
            val detail = details.optJSONObject(index) ?: continue
            detailsByCourse.getOrPut(detail.optInt("id")) { mutableListOf() }.add(detail)
        }

        val courses = mutableListOf<ImportedCourse>()
        for (index in 0 until bases.length()) {
            val base = bases.optJSONObject(index) ?: continue
            val courseId = base.optInt("id")
            val courseName = base.optString("courseName").trim().ifBlank { "未命名课程" }
            val courseDetails = detailsByCourse[courseId].orEmpty()
            if (courseDetails.isEmpty()) continue

            val sessions = courseDetails.map { detail ->
                val startWeek = detail.optInt("startWeek", 1).coerceAtLeast(1)
                val endWeek = detail.optInt("endWeek", 20).coerceAtLeast(startWeek)
                ImportedSession(
                    dayOfWeek = detail.optInt("day", 1).coerceIn(1, 7),
                    startPeriod = detail.optInt("startNode", 1).coerceAtLeast(1),
                    periodCount = detail.optInt("step", 1).coerceIn(1, 20),
                    startWeek = startWeek,
                    endWeek = endWeek,
                    parity = detail.optInt("type", 0).coerceIn(0, 2),
                    room = detail.optString("room").takeIf { it.isNotBlank() },
                )
            }

            val teacher = courseDetails
                .mapNotNull { it.optString("teacher").takeIf { teacherName -> teacherName.isNotBlank() } }
                .firstOrNull()

            courses.add(
                ImportedCourse(
                    name = courseName,
                    teacher = teacher,
                    colorIndex = (courseName.hashCode().absoluteValue) % 6,
                    sessions = sessions,
                ),
            )
        }

        return ImportedTimetable(name = name, courses = courses)
    }

    private fun normalizeColor(value: Int): Int = ((value % 6) + 6) % 6
}
