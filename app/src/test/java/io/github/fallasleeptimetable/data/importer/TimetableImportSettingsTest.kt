package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.CourseEntity
import io.github.fallasleeptimetable.data.CourseSessionEntity
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.data.TimetableEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TimetableImportSettingsTest {

    private val term = TermEntity(
        id = 1,
        name = "2026 秋",
        startDateEpochDay = LocalDate.of(2026, 9, 7).toEpochDay(),
        totalWeeks = 20,
        periodsPerDay = 4,
        showSaturday = true,
        showSunday = false,
    )

    private val slots = listOf(
        TimeSlotEntity(termId = 1, period = 1, startMinute = 480, endMinute = 525),
        TimeSlotEntity(termId = 1, period = 2, startMinute = 535, endMinute = 580),
        TimeSlotEntity(termId = 1, period = 3, startMinute = 600, endMinute = 645),
        TimeSlotEntity(termId = 1, period = 4, startMinute = 655, endMinute = 700),
    )

    private val courses = listOf(
        CourseWithSessions(
            course = CourseEntity(id = 1, timetableId = 1, name = "高等数学"),
            sessions = listOf(
                CourseSessionEntity(
                    id = 1,
                    courseId = 1,
                    dayOfWeek = 1,
                    startPeriod = 1,
                    periodCount = 2,
                    startWeek = 1,
                    endWeek = 16,
                    parity = 0,
                    room = "教三-201",
                ),
            ),
        ),
    )

    private fun exportedJson(): String = TimetableExporter.buildJson(
        term = term,
        timetable = TimetableEntity(id = 1, termId = 1, name = "主课表"),
        courses = courses,
        timeSlots = slots,
    )

    @Test
    fun parsesTermAndTimeSlotsFromBackup() {
        val parsed = TimetableImporter.parse(exportedJson())

        assertEquals(term.startDateEpochDay, parsed.term?.startDateEpochDay)
        assertEquals(20, parsed.term?.totalWeeks)
        assertEquals(4, parsed.term?.periodsPerDay)
        assertEquals(true, parsed.term?.showSaturday)
        assertEquals(false, parsed.term?.showSunday)

        assertEquals(4, parsed.timeSlots?.size)
        val first = parsed.timeSlots!!.first()
        assertEquals(1, first.period)
        assertEquals(480, first.startMinute)
        assertEquals(525, first.endMinute)
    }

    @Test
    fun inspectReportsSettingsPresent() {
        val summary = TimetableImporter.inspect(exportedJson())

        assertTrue(summary != null && summary.hasSettings)
        assertEquals(term.startDateEpochDay, summary?.startDateEpochDay)
        assertEquals(4, summary?.periodsPerDay)
        assertEquals(4, summary?.timeSlotCount)
    }

    @Test
    fun inspectReturnsNullForJsonWithoutSettings() {
        val minimal = """
            {
              "timetable": {"name": "旧备份"},
              "courses": [
                {"name": "大学英语", "sessions": [{"dayOfWeek": 2, "startPeriod": 3}]}
              ]
            }
        """.trimIndent()

        assertNull(TimetableImporter.inspect(minimal))
    }

    @Test
    fun dropsInvalidTimeSlots() {
        val json = """
            {
              "timetable": {"name": "损坏的备份"},
              "courses": [
                {"name": "体育", "sessions": [{"dayOfWeek": 4, "startPeriod": 5}]}
              ],
              "timeSlots": [
                {"period": 1, "startMinute": 480, "endMinute": 525},
                {"period": 0, "startMinute": 100, "endMinute": 200},
                {"period": 2, "startMinute": 600, "endMinute": 500},
                {"period": 3, "startMinute": 600, "endMinute": 2000}
              ]
            }
        """.trimIndent()

        val parsed = TimetableImporter.parse(json)

        assertEquals(1, parsed.timeSlots?.size)
        assertEquals(1, parsed.timeSlots?.first()?.period)
    }
}
