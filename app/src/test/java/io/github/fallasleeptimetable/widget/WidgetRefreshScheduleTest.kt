package io.github.fallasleeptimetable.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class WidgetRefreshScheduleTest {

    @Test
    fun picksEarliestUpcomingClassEnd() {
        val now = LocalDateTime.of(2026, 10, 31, 9, 0)
        val target = nextWidgetRefreshAt(now, listOf(580, 525, 700))

        assertEquals(LocalDateTime.of(2026, 10, 31, 9, 40), target)
    }

    @Test
    fun refreshesAtCurrentMinuteAsClassEnded() {
        val now = LocalDateTime.of(2026, 10, 31, 9, 40)
        val target = nextWidgetRefreshAt(now, listOf(580, 700))

        assertEquals(LocalDateTime.of(2026, 10, 31, 11, 40), target)
    }

    @Test
    fun fallsBackToNextMidnightAfterLastClass() {
        val now = LocalDateTime.of(2026, 10, 31, 18, 0)
        val target = nextWidgetRefreshAt(now, listOf(580, 700))

        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), target)
    }

    @Test
    fun fallsBackToNextMidnightWithoutClass() {
        val now = LocalDateTime.of(2026, 10, 31, 8, 0)
        val target = nextWidgetRefreshAt(now, emptyList())

        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), target)
    }
}
