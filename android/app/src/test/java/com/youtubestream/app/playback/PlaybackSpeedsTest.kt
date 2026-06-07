package com.youtubestream.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedsTest {
    @Test fun `cycles 1 to 1_25 to 1_5 to 2 back to 1`() {
        assertEquals(1.25f, nextPlaybackSpeed(1f))
        assertEquals(1.5f, nextPlaybackSpeed(1.25f))
        assertEquals(2f, nextPlaybackSpeed(1.5f))
        assertEquals(1f, nextPlaybackSpeed(2f))
    }
    @Test fun `unknown speed snaps to 1`() = assertEquals(1f, nextPlaybackSpeed(0.7f))
    @Test fun `formats without trailing zeros`() {
        assertEquals("1×", formatSpeed(1f))
        assertEquals("1.25×", formatSpeed(1.25f))
        assertEquals("1.5×", formatSpeed(1.5f))
        assertEquals("2×", formatSpeed(2f))
    }
}
