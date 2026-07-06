package com.youtubestream.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun track(id: String) = PlayableTrack(mediaId = id, uri = "file:///$id", title = id, artist = "artist")
private fun ctx(id: String) = QueueEntry(track(id), isManual = false)
private fun man(id: String) = QueueEntry(track(id), isManual = true)

/** Pure-JVM tests for the Spotify-style two-tier queue math. */
class TwoTierQueueTest {

    // --- pendingCount: the manual block is DERIVED (contiguous manual run after current) ---

    @Test fun `pending is the contiguous manual run after current`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("b"), man("stray"))
        assertEquals(2, TwoTierQueue.pendingCount(q, 0))
    }

    @Test fun `pending is zero when the next item is context`() {
        assertEquals(0, TwoTierQueue.pendingCount(listOf(ctx("a"), ctx("b"), man("m")), 0))
    }

    @Test fun `previous re-pends a played manual item without any bookkeeping`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("b"))
        assertEquals(1, TwoTierQueue.pendingCount(q, 1))  // playing m1 → only m2 pending
        assertEquals(2, TwoTierQueue.pendingCount(q, 0))  // back on a → m1 AND m2 pending again
    }

    @Test fun `pending is zero at the end of the queue`() {
        assertEquals(0, TwoTierQueue.pendingCount(listOf(ctx("a")), 0))
    }

    // --- insert indices ---

    @Test fun `addToQueue inserts at the back of the manual block`() {
        val q = listOf(ctx("a"), man("m1"), ctx("b"))
        assertEquals(2, TwoTierQueue.addToQueueIndex(q, 0))
    }

    @Test fun `addToQueue with no block inserts right after current`() {
        assertEquals(1, TwoTierQueue.addToQueueIndex(listOf(ctx("a"), ctx("b")), 0))
    }

    @Test fun `playNext inserts immediately after current`() {
        assertEquals(3, TwoTierQueue.playNextIndex(2))
    }

    // --- move: positional move + the drag crosses the queue/context boundary ---

    @Test fun `context item dragged into the block becomes manual`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("x"), ctx("y"))
        val moved = TwoTierQueue.move(q, 0, from = 3, to = 2)
        assertEquals(listOf("a", "m1", "x", "m2", "y"), moved.map { it.track.mediaId })
        assertTrue(moved[2].isManual)
    }

    @Test fun `manual item dragged below the block leaves the queue`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("x"))
        val moved = TwoTierQueue.move(q, 0, from = 2, to = 3)
        assertEquals(listOf("a", "m1", "x", "m2"), moved.map { it.track.mediaId })
        assertFalse(moved[3].isManual)
    }

    @Test fun `swapping two queued items keeps both manual`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("x"))
        val moved = TwoTierQueue.move(q, 0, from = 1, to = 2)
        assertEquals(listOf("a", "m2", "m1", "x"), moved.map { it.track.mediaId })
        assertTrue(moved[1].isManual)
        assertTrue(moved[2].isManual)
    }

    @Test fun `context item dropped just past the block stays context`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("x"), ctx("y"))
        val moved = TwoTierQueue.move(q, 0, from = 4, to = 3)
        assertEquals(listOf("a", "m1", "m2", "y", "x"), moved.map { it.track.mediaId })
        assertFalse(moved[3].isManual)
    }

    @Test fun `move ignores out-of-range and identity moves`() {
        val q = listOf(ctx("a"), ctx("b"))
        assertEquals(q, TwoTierQueue.move(q, 0, 1, 1))
        assertEquals(q, TwoTierQueue.move(q, 0, 5, 0))
    }
}
