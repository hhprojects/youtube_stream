package com.youtubestream.app.playback

import kotlin.random.Random

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

    /** A rebuilt timeline for setQueueAndPlay. [originalOrder] = canonical upcoming ids for un-shuffle. */
    data class SetQueuePlan(
        val entries: List<QueueEntry>,
        val startIndex: Int,
        val originalOrder: List<String>,
    )

    /**
     * New playing context with Spotify queue survival: the still-pending manual block is re-inserted
     * right after the new start item. Plain: context up to startIndex, block, rest. Shuffled
     * (shuffle-play): a random start track, the block, then the rest permuted; [originalOrder]
     * records the canonical order so un-shuffle can restore it.
     */
    fun buildSetQueue(
        oldEntries: List<QueueEntry>,
        oldCurrentIndex: Int,
        context: List<PlayableTrack>,
        startIndex: Int,
        shuffled: Boolean,
        random: Random,
    ): SetQueuePlan {
        val pending = oldEntries.drop(oldCurrentIndex + 1).takeWhile { it.isManual }
        if (context.isEmpty()) return SetQueuePlan(pending, 0, emptyList())
        return if (shuffled) {
            val start = random.nextInt(context.size)
            val rest = (context.take(start) + context.drop(start + 1)).shuffled(random)
            SetQueuePlan(
                entries = listOf(QueueEntry(context[start], isManual = false)) + pending +
                    rest.map { QueueEntry(it, isManual = false) },
                startIndex = 0,
                originalOrder = context.map { it.mediaId },
            )
        } else {
            val at = startIndex.coerceIn(0, context.lastIndex)
            SetQueuePlan(
                entries = context.take(at + 1).map { QueueEntry(it, isManual = false) } + pending +
                    context.drop(at + 1).map { QueueEntry(it, isManual = false) },
                startIndex = at,
                originalOrder = emptyList(),
            )
        }
    }

    /**
     * Jump to an up-next row. [moveCount] == 0 → plain seek. Otherwise move the manual block
     * [moveFrom, moveFrom+moveCount) to [moveNewIndex] — expressed in Media3's post-removal
     * coordinates (MediaController.moveMediaItems semantics) — then seek [seekIndex].
     */
    data class JumpPlan(val moveFrom: Int, val moveCount: Int, val moveNewIndex: Int, val seekIndex: Int)

    /**
     * Tapping a context item pulls the pending block along so queued songs still play next
     * (Spotify). Tapping inside the block just seeks — skipped queued items fall into history.
     */
    fun jumpPlan(entries: List<QueueEntry>, currentIndex: Int, target: Int): JumpPlan {
        val blockLen = pendingCount(entries, currentIndex)
        val blockStart = currentIndex + 1
        return if (blockLen == 0 || target < blockStart + blockLen) {
            JumpPlan(moveFrom = 0, moveCount = 0, moveNewIndex = 0, seekIndex = target)
        } else {
            JumpPlan(
                moveFrom = blockStart,
                moveCount = blockLen,
                moveNewIndex = target - blockLen + 1,
                seekIndex = target - blockLen,
            )
        }
    }

    /** Replacement for the upcoming context tail — everything after the manual block. */
    data class TailPlan(val tailStart: Int, val newTail: List<QueueEntry>)

    /** Shuffle ON: permute the context tail. History, the current item, and the block never move. */
    fun shuffleTail(entries: List<QueueEntry>, currentIndex: Int, random: Random): TailPlan {
        val tailStart = currentIndex + 1 + pendingCount(entries, currentIndex)
        return TailPlan(tailStart, entries.drop(tailStart).shuffled(random))
    }

    /**
     * Shuffle OFF: reorder the tail back to [originalOrder]'s relative order. Duplicate ids are
     * matched greedily; ids not listed (shouldn't happen — inserts while shuffled go to the manual
     * block) keep their position at the end.
     */
    fun unshuffleTail(entries: List<QueueEntry>, currentIndex: Int, originalOrder: List<String>): TailPlan {
        val tailStart = currentIndex + 1 + pendingCount(entries, currentIndex)
        val remaining = entries.drop(tailStart).toMutableList()
        val restored = mutableListOf<QueueEntry>()
        for (id in originalOrder) {
            val i = remaining.indexOfFirst { it.track.mediaId == id }
            if (i >= 0) restored += remaining.removeAt(i)
        }
        restored += remaining
        return TailPlan(tailStart, restored)
    }

    /**
     * Sticky-shuffle policy for a new queue: an explicit shuffle-play always wins; otherwise a lit
     * toggle carries over to a music queue (podcasts always play in publication order).
     */
    fun effectiveShuffle(requested: Boolean, shuffleOn: Boolean, context: List<PlayableTrack>): Boolean =
        requested || (shuffleOn && context.isNotEmpty() && context.none { it.isPodcast })
}
