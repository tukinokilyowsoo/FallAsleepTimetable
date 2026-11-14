package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.CourseEntity
import io.github.fallasleeptimetable.data.CourseSessionEntity
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class IcsTimetableTest {

    private val term = TermEntity(
        id = 1,
        name = "2026 秋",
        startDateEpochDay = LocalDate.of(2026, 9, 7).toEpochDay(),
        totalWeeks = 20,
        periodsPerDay = 12,
    )

    private val slots = listOf(
        slot(1, 480, 525),
        slot(2, 535, 580),
        slot(3, 600, 645),
        slot(4, 655, 700),
        slot(5, 840, 885),
        slot(6, 895, 940),
    )

    private fun slot(period: Int, start: Int, end: Int) = TimeSlotEntity(
        timetableId = term.id,
        period = period,
        startMinute = start,
        endMinute = end,
    )

    private fun courses(name: String = "高等数学, 上") = listOf(
        CourseWithSessions(
            course = CourseEntity(
                id = 10,
                timetableId = 1,
                name = name,
                teacher = "张老师",
                colorIndex = 2,
            ),
            sessions = listOf(
                CourseSessionEntity(
                    id = 100,
                    courseId = 10,
                    dayOfWeek = 1,
                    startPeriod = 1,
                    periodCount = 2,
                    startWeek = 1,
                    endWeek = 16,
                    parity = 0,
                    room = "教三-201",
                ),
                CourseSessionEntity(
                    id = 101,
                    courseId = 10,
                    dayOfWeek = 3,
                    startPeriod = 3,
                    periodCount = 2,
                    startWeek = 1,
                    endWeek = 15,
                    parity = 1,
                    room = null,
                ),
            ),
        ),
    )

    @Test
    fun exportsValidCalendarWithEscapingAndFolding() {
        val ics = IcsExporter.build(
            term = term,
            timetableName = "我的课表",
            courses = courses(name = "高等数学（面向人工智能与数据科学的超长课程名称测试）"),
            timeSlots = slots,
            now = Instant.parse("2026-10-31T04:00:00Z"),
        )

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(ics.contains("X-WR-CALNAME:我的课表"))
        assertEquals(2, Regex("BEGIN:VEVENT").findAll(ics).count())
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;INTERVAL=1;COUNT=16;WKST=MO"))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=8;WKST=MO"))
        assertTrue(ics.contains("DTSTART:20260907T080000"))
        assertTrue(ics.contains("DTEND:20260907T094000"))
        assertTrue(ics.contains("LOCATION:教三-201"))
        assertTrue(ics.contains("X-FALLASLEEP-PARITY:1"))

        ics.split("\r\n").filter { it.isNotBlank() }.forEach { line ->
            assertTrue(
                "行长超过 75 字节：$line",
                line.toByteArray(Charsets.UTF_8).size <= 75,
            )
        }
    }

    @Test
    fun roundTripsOwnCalendarExactly() {
        val ics = IcsExporter.build(
            term = term,
            timetableName = "我的课表",
            courses = courses(),
            timeSlots = slots,
        )

        val parsed = IcsImporter.parse(ics, term, slots)

        assertEquals("我的课表", parsed.name)
        val course = parsed.courses.single()
        assertEquals("高等数学, 上", course.name)
        assertEquals("张老师", course.teacher)

        val session = course.sessions.first { it.dayOfWeek == 1 }
        assertEquals(1, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(16, session.endWeek)
        assertEquals(0, session.parity)
        assertEquals("教三-201", session.room)

        val odd = course.sessions.first { it.dayOfWeek == 3 }
        assertEquals(3, odd.startPeriod)
        assertEquals(2, odd.periodCount)
        assertEquals(1, odd.startWeek)
        assertEquals(15, odd.endWeek)
        assertEquals(1, odd.parity)
    }

    @Test
    fun importsThirdPartyWeeklyEvent() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Other App//CN
            X-WR-CALNAME:外部日历
            BEGIN:VEVENT
            SUMMARY:大学英语
            DTSTART;TZID=Asia/Shanghai:20260908T100000
            DTEND;TZID=Asia/Shanghai:20260908T114000
            RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=6;WKST=MO
            LOCATION:外语楼305
            DESCRIPTION:教师: 李老师\n第3-4节
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val parsed = IcsImporter.parse(
            ics,
            term,
            slots,
            systemZone = java.time.ZoneId.of("Asia/Shanghai"),
        )

        assertEquals("外部日历", parsed.name)
        val course = parsed.courses.single()
        assertEquals("大学英语", course.name)
        assertEquals("李老师", course.teacher)
        val session = course.sessions.single()
        assertEquals(2, session.dayOfWeek)
        assertEquals(3, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(11, session.endWeek)
        assertEquals(1, session.parity)
        assertEquals("外语楼305", session.room)
    }

    @Test
    fun mergesIndividualWeeklyEventsIntoOneSession() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            SUMMARY:数据结构
            DTSTART:20260907T080000
            DTEND:20260907T094000
            LOCATION:计算机楼401
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:数据结构
            DTSTART:20260914T080000
            DTEND:20260914T094000
            LOCATION:计算机楼401
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:数据结构
            DTSTART:20260921T080000
            DTEND:20260921T094000
            LOCATION:计算机楼401
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val parsed = IcsImporter.parse(ics, term, slots)

        val session = parsed.courses.single().sessions.single()
        assertEquals(1, session.startWeek)
        assertEquals(3, session.endWeek)
        assertEquals(1, session.dayOfWeek)
        assertEquals(1, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals("计算机楼401", session.room)
    }

    @Test
    fun ignoresAllDayAndOutOfTermEvents() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            SUMMARY:全天事件
            DTSTART;VALUE=DATE:20260908
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:学期外课程
            DTSTART:20270601T080000
            DTEND:20270601T093000
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:有效课程
            DTSTART:20260909T080000
            DTEND:20260909T093000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val parsed = IcsImporter.parse(ics, term, slots)

        val course = parsed.courses.single()
        assertEquals("有效课程", course.name)
        assertFalse(course.sessions.any { it.dayOfWeek == 6 })
        assertEquals(3, course.sessions.single().dayOfWeek)
    }
}
