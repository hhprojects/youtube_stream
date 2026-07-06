package com.youtubestream.app.playback

/** One timeline entry: the track plus whether the user queued it manually ("Next in queue"). */
data class QueueEntry(val track: PlayableTrack, val isManual: Boolean)

/**
 * Pure index math for the Spotify-style two-tier queue. The manual block is DERIVED, never stored:
 * it is the contiguous run of manual entries starting right after the current item. Flags are
 * static per entry — playback moving (next/previous/auto-advance) needs no bookkeeping, because a
 * played manual entry simply stops being "after current" (and previous naturally re-pends it).
 * All functions are pure with zero Android imports; PlaybackConnection applies the results to the
 * MediaController.
 */
object TwoTierQueue {

    /** Length of the contiguous manual run right after [currentIndex] — the "Next in queue" block. */
    fun pendingCount(entries: List<QueueEntry>, currentIndex: Int): Int =
        entries.drop(currentIndex + 1).takeWhile { it.isManual }.size

    /** Insert position for add-to-queue: the back of the manual block. */
    fun addToQueueIndex(entries: List<QueueEntry>, currentIndex: Int): Int =
        currentIndex + 1 + pendingCount(entries, currentIndex)

    /** Insert position for play-next: the front of the manual block. */
    fun playNextIndex(currentIndex: Int): Int = currentIndex + 1

    /**
     * A drag's positional move plus the boundary reflag: the moved entry adopts the tier it lands
     * in. Threshold = the manual block computed WITHOUT the moved entry, extended by one slot when
     * the moved entry is itself manual — so nudging a queued item one slot down keeps it queued,
     * while a context item must land strictly INSIDE the old block to join it (ties go to context).
     * Moves touching the history region (≤ currentIndex — unreachable from the queue UI) keep
     * flags untouched.
     */
    fun move(entries: List<QueueEntry>, currentIndex: Int, from: Int, to: Int): List<QueueEntry> {
        if (from !in entries.indices || to !in entries.indices || from == to) return entries
        val moved = entries[from]
        val result = entries.toMutableList().apply { add(to, removeAt(from)) }
        if (from <= currentIndex || to <= currentIndex) return result
        val without = entries.toMutableList().apply { removeAt(from) }
        val threshold = currentIndex + pendingCount(without, currentIndex) + if (moved.isManual) 1 else 0
        result[to] = moved.copy(isManual = to <= threshold)
        return result
    }
}
