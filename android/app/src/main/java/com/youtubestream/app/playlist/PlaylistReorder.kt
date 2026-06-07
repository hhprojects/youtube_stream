package com.youtubestream.app.playlist

/**
 * Pure list-move backing drag-to-reorder. Returns a NEW list with the item at [from]
 * moved to index [to]. Indices are assumed valid (they come from visible list positions).
 *
 * `add(to, removeAt(from))` handles both directions: removeAt shifts the tail left, then
 * [to] indexes into the post-removal list — which is exactly where the dragged item should land.
 */
object PlaylistReorder {
    fun reorder(ids: List<String>, from: Int, to: Int): List<String> {
        if (from == to) return ids
        return ids.toMutableList().apply { add(to, removeAt(from)) }
    }
}
