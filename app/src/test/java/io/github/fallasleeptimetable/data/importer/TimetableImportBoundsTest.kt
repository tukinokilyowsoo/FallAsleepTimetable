package io.github.fallasleeptimetable.data.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableImportBoundsTest {

    @Test
    fun clampsOutOfRangeSessions() {
        val json = """
            {
              "app": "FallAsleep",
              "courses": [
                {
                  "name": "测试课程",
                  "sessions": [
                    {
                      "dayOfWeek": 99,
                      "startPeriod": 999,
                      "periodCount": 999,
                      "startWeek": 0,
                      "endWeek": 999,
                      "parity": 9
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val session = TimetableImporter.parse(json).courses.single().sessions.single()

        assertEquals(7, session.dayOfWeek)
        assertEquals(30, session.startPeriod)
        assertEquals(1, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(60, session.endWeek)
        assertEquals(2, session.parity)
    }

    @Test
    fun keepsValidSessionValues() {
        val json = """
            {
              "courses": [
                {
                  "name": "高等数学",
                  "sessions": [
                    {
                      "dayOfWeek": 3,
                      "startPeriod": 5,
                      "periodCount": 2,
                      "startWeek": 1,
                      "endWeek": 16,
                      "parity": 1
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val session = TimetableImporter.parse(json).courses.single().sessions.single()

        assertEquals(3, session.dayOfWeek)
        assertEquals(5, session.startPeriod)
        assertEquals(2, session.periodCount)
        assertEquals(1, session.startWeek)
        assertEquals(16, session.endWeek)
        assertEquals(1, session.parity)
    }
}
