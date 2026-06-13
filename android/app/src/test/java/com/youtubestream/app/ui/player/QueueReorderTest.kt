package com.youtubestream.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueReorderTest {
    // moveItem must match Media3 Player.moveMediaItem(from, to): the element at `from` ends up at `to`.

    @Test fun `move forward (from less than to)`() {
        // moveMediaItem(0, 2) on [a,b,c,d] -> [b,c,a,d]
        assertEquals(listOf("b", "c", "a", "d"), moveItem(listOf("a", "b", "c", "d"), 0, 2))
    }

    @Test fun `move backward (from greater than to)`() {
        // moveMediaItem(3, 1) on [a,b,c,d] -> [a,d,b,c]
        assertEquals(listOf("a", "d", "b", "c"), moveItem(listOf("a", "b", "c", "d"), 3, 1))
    }

    @Test fun `move to adjacent`() {
        assertEquals(listOf("b", "a", "c"), moveItem(listOf("a", "b", "c"), 0, 1))
    }

    @Test fun `same index is a no-op`() {
        assertEquals(listOf("a", "b", "c"), moveItem(listOf("a", "b", "c"), 1, 1))
    }

    @Test fun `out-of-range indices are a no-op`() {
        assertEquals(listOf("a", "b"), moveItem(listOf("a", "b"), 0, 5))
        assertEquals(listOf("a", "b"), moveItem(listOf("a", "b"), -1, 1))
    }

    @Test fun `duplicates are preserved positionally`() {
        // The queue can hold the same id twice; moving by index must not collapse them.
        // moveMediaItem(0, 2) on [a,a,b]: the first 'a' moves to index 2 -> [a,b,a].
        assertEquals(listOf("a", "b", "a"), moveItem(listOf("a", "a", "b"), 0, 2))
    }
}
