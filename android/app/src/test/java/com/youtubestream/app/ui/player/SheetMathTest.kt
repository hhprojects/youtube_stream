package com.youtubestream.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // --- shouldCollapse: 3-way release rule (down-flick / up-flick / gentle distance tiebreak) ---
    // velocityThresholdPx is a fixed 600f in these tests; positionalThreshold a fixed 0.8f.

    @Test fun `down-flick collapses even when barely dragged`() {
        // Near fully-expanded, but a decisive downward flick → collapse. The core "less drag" fix.
        assertTrue(shouldCollapse(progress = 0.95f, velocityY = 1000f, velocityThresholdPx = 600f, positionalThreshold = 0.8f))
    }

    @Test fun `up-flick stays expanded even when dragged past the distance line`() {
        // Dragged down past 0.8 (progress 0.6) then flicked back UP to cancel → must stay expanded.
        assertFalse(shouldCollapse(progress = 0.6f, velocityY = -1000f, velocityThresholdPx = 600f, positionalThreshold = 0.8f))
    }

    @Test fun `gentle release past the distance threshold collapses`() {
        // Negligible velocity, dragged down past ~20% (progress 0.7 < 0.8) → collapse.
        assertTrue(shouldCollapse(progress = 0.7f, velocityY = 0f, velocityThresholdPx = 600f, positionalThreshold = 0.8f))
    }

    @Test fun `gentle release short of the distance threshold snaps back`() {
        // Negligible velocity, barely dragged (progress 0.95 > 0.8) → stay expanded.
        assertFalse(shouldCollapse(progress = 0.95f, velocityY = 0f, velocityThresholdPx = 600f, positionalThreshold = 0.8f))
    }

    @Test fun `down-velocity just below threshold falls through to distance`() {
        // Under the flick threshold, progress above the line → not a flick; distance says stay expanded.
        assertFalse(shouldCollapse(progress = 0.95f, velocityY = 599f, velocityThresholdPx = 600f, positionalThreshold = 0.8f))
    }

    @Test fun `down-velocity just above threshold collapses regardless of distance`() {
        assertTrue(shouldCollapse(progress = 0.95f, velocityY = 601f, velocityThresholdPx = 600f, positionalThreshold = 0.8f))
    }
}
