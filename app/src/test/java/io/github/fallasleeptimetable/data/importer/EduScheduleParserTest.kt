package io.github.fallasleeptimetable.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EduScheduleParserTest {

    @Test
    fun parsesNewFzPage() {
        val result = EduScheduleParser.parse(NEW_FZ_HTML)

        assertEquals(EduTemplate.NEW_FZ, result.template)
        val course = result.timetable.courses.single()
        assertEquals("高等数学", course.name)
        assertEquals("张老师", course.teacher)
        val session = course.sessions.single()
        assertEquals(1, session.dayOfWeek)
        assertEquals(1, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(16, session.endWeek)
        assertEquals(0, session.parity)
        assertEquals("教三-201", session.room)
    }

    @Test
    fun parsesOldFzPage() {
        val result = EduScheduleParser.parse(OLD_FZ_HTML)

        assertEquals(EduTemplate.FZ, result.template)
        val course = result.timetable.courses.single()
        assertEquals("高等数学", course.name)
        assertEquals("张老师", course.teacher)
        val session = course.sessions.single()
        assertEquals(1, session.dayOfWeek)
        assertEquals(1, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(16, session.endWeek)
        assertEquals("教三-201", session.room)
    }

    @Test
    fun parsesQzPage() {
        val result = EduScheduleParser.parse(QZ_HTML)

        assertEquals(EduTemplate.QZ, result.template)
        val course = result.timetable.courses.single()
        assertEquals("大学英语", course.name)
        assertEquals("李老师", course.teacher)
        val session = course.sessions.single()
        assertEquals(1, session.dayOfWeek)
        assertEquals(1, session.startPeriod)
        assertEquals(1, session.startWeek)
        assertEquals(16, session.endWeek)
    }

    @Test
    fun rejectsUnknownPage() {
        val failure = runCatching { EduScheduleParser.parse("<html><body>hello</body></html>") }
        assertTrue(failure.isFailure)
    }

    private companion object {
        const val NEW_FZ_HTML = """
            <html><body>
              <table id="table1">
                <tr>
                  <td class="festival">1</td>
                  <td id="1-1">
                    <div>
                      <span class="title">高等数学</span>
                      <p title="教师"><font>张老师</font></p>
                      <p title="上课地点"><font>教三-201</font></p>
                      <p title="节/周"><font>1-2节(1-16周)</font></p>
                    </div>
                  </td>
                </tr>
              </table>
            </body></html>
        """

        const val OLD_FZ_HTML = """
            <html><body>
              <table id="Table1">
                <tr>
                  <td>第1节</td>
                  <td>高等数学 必修 周一第1-2节{第1-16周} 张老师 教三-201</td>
                </tr>
              </table>
            </body></html>
        """

        const val QZ_HTML = """
            <html><body>
              <table id="kbtable">
                <tr>
                  <td>
                    <div class="kbcontent">
                      大学英语<br>
                      <font title="老师">李老师</font>
                      <font title="教室">外语楼-305</font>
                      <font title="周次(节次)">1-16(周)</font>
                    </div>
                  </td>
                </tr>
              </table>
            </body></html>
        """
    }
}
