package io.github.fallasleeptimetable.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryTimetableTest {
    @Test
    fun readsDeclaredWeekdaysPeriodsAndRoomsFromTheorySchedule() {
        val result = EduScheduleParser.parse(THEORY_HTML)
        assertEquals(EduTemplate.QZ, result.template)
        assertEquals(listOf("工程数学", "实验课"), result.timetable.courses.map { it.name })

        val mathematics = result.timetable.courses[0]
        assertEquals("李老师", mathematics.teacher)
        assertEquals(
            listOf(
                ImportedSession(1, 3, 2, 2, 5, 0, "A101"),
                ImportedSession(1, 3, 2, 7, 7, 0, "A101"),
                ImportedSession(1, 3, 2, 12, 15, 0, "A101"),
                ImportedSession(1, 3, 2, 18, 18, 0, "B203"),
            ),
            mathematics.sessions,
        )
        assertEquals(
            listOf(ImportedSession(7, 10, 1, 1, 9, 1, "实验楼")),
            result.timetable.courses[1].sessions,
        )
    }

    @Test
    fun doesNotImportTheFooterOrInventWeeksForAnIncompleteCourse() {
        val result = EduScheduleParser.parse(
            THEORY_HTML.replace("2-5,7,12-15(周)[03-04节]", "(周)[03-04节]")
                .replace("18(周)[03-04节]", "18(周)"),
        )
        assertEquals("实验课", result.timetable.courses.single().name)
        assertTrue(result.timetable.courses.none { it.name == "活动信息" || it.name == "旧的打印副本" })
    }

    private companion object {
        const val THEORY_HTML = """
            <table id="timetable">
              <tr><th>&nbsp;</th><th>星期一</th><th>星期日</th></tr>
              <tr><th>第一二节<br>08:00-09:40</th>
                <td><input type="hidden" value="ID">
                  <div class="kbcontent1"><font>旧的打印副本</font></div>
                  <div class="kbcontent">
                    <div class="item-box"><img></div><font>工程数学</font><br>
                    <div class="item-box"><img></div><font title="教师">李老师</font><br>
                    <font title="周次(节次)">2-5,7,12-15(周)[03-04节]</font><br>
                    <font>【一班】</font><font title="教室">A101</font><br>
                    ----------------------<br>
                    <div class="item-box"><img></div><font>工程数学</font><br>
                    <font title="教师">李老师</font><br>
                    <font title="周次(节次)">18(周)[03-04节]</font><br>
                    <font title="教室">B203</font>
                  </div>
                  <div class="kbcontent" style="display:none"></div>
                </td>
                <td><div class="kbcontent"><font>实验课</font><br>
                  <font title="周次(节次)">1–9(单周)［10节］</font><br>
                  <font title="教室">实验楼</font></div></td>
              </tr>
              <tr><th>备注</th><td colspan="2">活动信息</td></tr>
            </table>
        """
    }
}
