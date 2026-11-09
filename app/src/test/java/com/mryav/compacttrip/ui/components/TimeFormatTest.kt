package com.mryav.compacttrip.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun countdown_minutesThenWholeHoursThenWholeDays() {
        assertEquals(Countdown("Now", null), countdown(0))
        assertEquals(Countdown("1", "min"), countdown(1))
        assertEquals(Countdown("59", "min"), countdown(59))
        assertEquals(Countdown("1", "hr"), countdown(60))
        assertEquals(Countdown("1", "hr"), countdown(113)) // never "1hr 53"
        assertEquals(Countdown("23", "hr"), countdown(24 * 60 - 1))
        assertEquals(Countdown("1", "d"), countdown(24 * 60))
        assertEquals(Countdown("2", "d"), countdown(2 * 24 * 60 + 300))
    }

    @Test
    fun minutesUntil_roundsUpAndNeverNegative() {
        assertEquals(1, minutesUntil(departsAt = 30_000, now = 0))
        assertEquals(0, minutesUntil(departsAt = 0, now = 5_000))
        assertEquals(2, minutesUntil(departsAt = 120_000, now = 0))
    }
}
