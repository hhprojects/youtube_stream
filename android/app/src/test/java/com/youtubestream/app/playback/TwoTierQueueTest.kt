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

    // --- buildSetQueue: new context with Spotify queue survival ---

    @Test fun `new context with no pending block is plain`() {
        val plan = TwoTierQueue.buildSetQueue(emptyList(), 0, listOf(track("a"), track("b")), 1, shuffled = false, random = kotlin.random.Random(1))
        assertEquals(listOf("a", "b"), plan.entries.map { it.track.mediaId })
        assertTrue(plan.entries.none { it.isManual })
        assertEquals(1, plan.startIndex)
        assertEquals(emptyList<String>(), plan.originalOrder)
    }

    @Test fun `pending manual block survives a new context right after the start item`() {
        val old = listOf(ctx("old1"), man("q1"), man("q2"), ctx("old2"))
        val plan = TwoTierQueue.buildSetQueue(old, 0, listOf(track("n1"), track("n2"), track("n3")), 1, false, kotlin.random.Random(1))
        assertEquals(listOf("n1", "n2", "q1", "q2", "n3"), plan.entries.map { it.track.mediaId })
        assertEquals(listOf(false, false, true, true, false), plan.entries.map { it.isManual })
        assertEquals(1, plan.startIndex)
    }

    @Test fun `played manual items do not survive a new context`() {
        val old = listOf(ctx("a"), man("q1"), man("q2"))
        val plan = TwoTierQueue.buildSetQueue(old, 2, listOf(track("n1")), 0, false, kotlin.random.Random(1))  // playing q2 → nothing pending
        assertEquals(listOf("n1"), plan.entries.map { it.track.mediaId })
    }

    @Test fun `shuffled context starts at a random item with the block next and the rest permuted`() {
        val old = listOf(ctx("a"), man("q1"))
        val context = listOf(track("n1"), track("n2"), track("n3"), track("n4"))
        val plan = TwoTierQueue.buildSetQueue(old, 0, context, 0, shuffled = true, random = kotlin.random.Random(7))
        assertEquals(0, plan.startIndex)
        assertEquals(5, plan.entries.size)
        assertEquals("q1", plan.entries[1].track.mediaId)   // pending block sits right after the start item
        assertTrue(plan.entries[1].isManual)
        assertEquals(
            setOf("n1", "n2", "n3", "n4"),                  // permutation: every context id exactly once
            plan.entries.filterNot { it.isManual }.map { it.track.mediaId }.toSet(),
        )
        assertEquals(listOf("n1", "n2", "n3", "n4"), plan.originalOrder)  // canonical order kept for un-shuffle
    }

    @Test fun `empty context keeps only the pending block`() {
        val plan = TwoTierQueue.buildSetQueue(listOf(ctx("a"), man("q1")), 0, emptyList(), 0, false, kotlin.random.Random(1))
        assertEquals(listOf("q1"), plan.entries.map { it.track.mediaId })
        assertEquals(0, plan.startIndex)
    }

    // --- jumpPlan: tapping an up-next row pulls the queue block along (Spotify) ---

    @Test fun `jump to a context item pulls the block along`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("x"), ctx("y"))
        val plan = TwoTierQueue.jumpPlan(q, 0, target = 4)   // tap y
        assertEquals(1, plan.moveFrom)
        assertEquals(2, plan.moveCount)
        assertEquals(3, plan.moveNewIndex)   // post-removal coords: [a,x,y] → the block lands after y
        assertEquals(2, plan.seekIndex)      // y's post-removal index
    }

    @Test fun `jump inside the block just seeks`() {
        val q = listOf(ctx("a"), man("m1"), man("m2"), ctx("x"))
        val plan = TwoTierQueue.jumpPlan(q, 0, target = 2)
        assertEquals(0, plan.moveCount)
        assertEquals(2, plan.seekIndex)
    }

    @Test fun `jump with no block just seeks`() {
        val plan = TwoTierQueue.jumpPlan(listOf(ctx("a"), ctx("b")), 0, target = 1)
        assertEquals(0, plan.moveCount)
        assertEquals(1, plan.seekIndex)
    }

    // --- app-managed shuffle: permute/restore ONLY the context tail after the block ---

    @Test fun `shuffleTail permutes only the context after the block`() {
        val q = listOf(ctx("hist"), ctx("cur"), man("m1"), ctx("x"), ctx("y"), ctx("z"))
        val plan = TwoTierQueue.shuffleTail(q, 1, kotlin.random.Random(3))
        assertEquals(3, plan.tailStart)
        assertEquals(setOf("x", "y", "z"), plan.newTail.map { it.track.mediaId }.toSet())
        assertTrue(plan.newTail.none { it.isManual })
    }

    @Test fun `unshuffleTail restores the original relative order`() {
        val q = listOf(ctx("cur"), man("m1"), ctx("z"), ctx("x"), ctx("y"))
        val plan = TwoTierQueue.unshuffleTail(q, 0, originalOrder = listOf("x", "y", "z"))
        assertEquals(2, plan.tailStart)
        assertEquals(listOf("x", "y", "z"), plan.newTail.map { it.track.mediaId })
    }

    @Test fun `unshuffleTail keeps items missing from the original order at the end`() {
        val q = listOf(ctx("cur"), ctx("z"), ctx("new"), ctx("x"))
        val plan = TwoTierQueue.unshuffleTail(q, 0, originalOrder = listOf("x", "z"))
        assertEquals(listOf("x", "z", "new"), plan.newTail.map { it.track.mediaId })
    }

    @Test fun `unshuffleTail handles duplicate ids greedily`() {
        val q = listOf(ctx("cur"), ctx("b"), ctx("a"), ctx("a"))
        val plan = TwoTierQueue.unshuffleTail(q, 0, originalOrder = listOf("a", "a", "b"))
        assertEquals(listOf("a", "a", "b"), plan.newTail.map { it.track.mediaId })
    }

    // --- effectiveShuffle: sticky-shuffle policy for a new queue ---

    @Test fun `explicit shuffle request always wins`() {
        assertTrue(TwoTierQueue.effectiveShuffle(requested = true, shuffleOn = false, context = listOf(track("a"))))
    }

    @Test fun `a lit toggle carries over to a music queue`() {
        assertTrue(TwoTierQueue.effectiveShuffle(requested = false, shuffleOn = true, context = listOf(track("a"), track("b"))))
    }

    @Test fun `toggle off and no request stays natural`() {
        assertFalse(TwoTierQueue.effectiveShuffle(requested = false, shuffleOn = false, context = listOf(track("a"))))
    }

    @Test fun `the toggle never carries over to a podcast queue`() {
        val episodes = listOf(track("e1").copy(isPodcast = true), track("e2").copy(isPodcast = true))
        assertFalse(TwoTierQueue.effectiveShuffle(requested = false, shuffleOn = true, context = episodes))
    }

    @Test fun `carry-over needs a non-empty context`() {
        assertFalse(TwoTierQueue.effectiveShuffle(requested = false, shuffleOn = true, context = emptyList()))
    }
}
