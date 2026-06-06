package com.youtubestream.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetMathTest {
    @Test fun `collapsed offset is progress 0`() {
        assertEquals(0f, progressFor(offset = 1000f, collapsedOffset = 1000f, expandedOffset = 0f))
    }

    @Test fun `expanded offset is progress 1`() {
        assertEquals(1f, progressFor(offset = 0f, collapsedOffset = 1000f, expandedOffset = 0f))
    }

    @Test fun `halfway offset is progress 0_5`() {
        assertEquals(0.5f, progressFor(offset = 500f, collapsedOffset = 1000f, expandedOffset = 0f))
    }

    @Test fun `overshoot past expanded clamps to 1`() {
        assertEquals(1f, progressFor(offset = -50f, collapsedOffset = 1000f, expandedOffset = 0f))
    }

    @Test fun `overshoot below collapsed clamps to 0`() {
        assertEquals(0f, progressFor(offset = 1200f, collapsedOffset = 1000f, expandedOffset = 0f))
    }

    @Test fun `degenerate zero span returns 0 not NaN`() {
        assertEquals(0f, progressFor(offset = 5f, collapsedOffset = 10f, expandedOffset = 10f))
    }
}
