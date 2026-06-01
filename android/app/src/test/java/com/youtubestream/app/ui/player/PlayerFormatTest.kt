package com.youtubestream.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerFormatTest {

    @Test fun formatsUnderAnHour() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:05", formatTime(5_000))
        assertEquals("1:05", formatTime(65_000))
        assertEquals("3:42", formatTime(222_000))
    }

    @Test fun formatsOverAnHour() {
        assertEquals("1:00:00", formatTime(3_600_000))
        assertEquals("1:01:05", formatTime(3_665_000))
    }

    @Test fun clampsNegativeAndUnknown() {
        assertEquals("0:00", formatTime(-1))
        assertEquals("0:00", formatTime(Long.MIN_VALUE))   // C.TIME_UNSET-style sentinel
    }
}
