package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.CourseEntity
import io.github.fallasleeptimetable.data.CourseSessionEntity
import io.github.fallasleeptimetable.data.CourseWithSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTimetableTest {

    private val courses = listOf(
        CourseWithSessions(
            course = CourseEntity(
                id = 1,
                timetableId = 1,
                name = "计算机, 网络",
                teacher = "王老师",
                colorIndex = 0,
            ),
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
                    room = "实验楼 \"A\" 201",
                ),
                CourseSessionEntity(
                    id = 2,
                    courseId = 1,
                    dayOfWeek = 5,
                    startPeriod = 3,
                    periodCount = 2,
                    startWeek = 2,
                    endWeek = 14,
                    parity = 2,
                    room = null,
                ),
            ),
        ),
    )

    @Test
    fun exportsUtf8BomAndEscapesFields() {
        val csv = CsvExporter.build(courses)

        assertTrue(csv.startsWith("\uFEFF课程名称,教师,教室,星期,开始节次,节数,开始周,结束周,单双周\r\n"))
        assertTrue(csv.contains("\"计算机, 网络\""))
        assertTrue(csv.contains("\"实验楼 \"\"A\"\" 201\""))
        assertTrue(csv.contains("周五,3,2,2,14,双周"))
    }

    @Test
    fun roundTripsOwnCsv() {
        val parsed = CsvImporter.parse(CsvExporter.build(courses))

        val course = parsed.courses.single()
        assertEquals("计算机, 网络", course.name)
        assertEquals("王老师", course.teacher)
        assertEquals(2, course.sessions.size)

        val monday = course.sessions.first { it.dayOfWeek == 1 }
        assertEquals(1, monday.startPeriod)
        assertEquals(2, monday.periodCount)
        assertEquals(1, monday.startWeek)
        assertEquals(16, monday.endWeek)
        assertEquals(0, monday.parity)
        assertEquals("实验楼 \"A\" 201", monday.room)

        val friday = course.sessions.first { it.dayOfWeek == 5 }
        assertEquals(3, friday.startPeriod)
        assertEquals(2, friday.startWeek)
        assertEquals(14, friday.endWeek)
        assertEquals(2, friday.parity)
    }

    @Test
    fun importsThirdPartyCsvWithAliasesAndRanges() {
        val csv = """
            课程,老师,地点,周几,节次,连节,周次,单双周
            线性代数,赵老师,教一101,周二,3-4,2,1-16,全周
            大学物理,钱老师,理学楼,星期四,5,2,1-15,单周
        """.trimIndent()

        val parsed = CsvImporter.parse(csv)

        assertEquals(2, parsed.courses.size)
        val linear = parsed.courses.first { it.name == "线性代数" }
        assertEquals("赵老师", linear.teacher)
        val session = linear.sessions.single()
        assertEquals(2, session.dayOfWeek)
        assertEquals(3, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(16, session.endWeek)
        assertEquals(0, session.parity)
        assertEquals("教一101", session.room)

        val physics = parsed.courses.first { it.name == "大学物理" }
        assertEquals(4, physics.sessions.single().dayOfWeek)
        assertEquals(1, physics.sessions.single().parity)
    }

    @Test
    fun skipsBrokenRowsButKeepsValidOnes() {
        val csv = """
            课程名称,星期,开始节次,节数,开始周,结束周
            高等数学,周一,1,2,1,16
            ,周二,1,2,1,16
            坏行,星期八,1,2,1,16
            大学英语,周三,3,2,1,16
        """.trimIndent()

        val parsed = CsvImporter.parse(csv)

        assertEquals(
            listOf("高等数学", "大学英语"),
            parsed.courses.map { it.name },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCsvWithoutRequiredColumns() {
        CsvImporter.parse("姓名,年龄\n张三,20")
    }
}
