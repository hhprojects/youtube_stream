package com.youtubestream.app.ui.player

/**
 * Pure list move matching Media3 `Player.moveMediaItem(from, to)`: remove the element at [from] and
 * re-insert it at [to]. Returns the list unchanged for a no-op or out-of-range indices.
 *
 * The player's up-next drag uses this for the dropped order; [com.youtubestream.app.playback.PlaybackConnection]
 * `moveQueueItem` applies the same move to the real timeline (offset by currentIndex + 1). Index-based (not
 * id-based) because the queue may hold the same track twice.
 */
fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> =
    if (from == to || from !in list.indices || to !in list.indices) {
        list
    } else {
        list.toMutableList().apply { add(to, removeAt(from)) }
    }
