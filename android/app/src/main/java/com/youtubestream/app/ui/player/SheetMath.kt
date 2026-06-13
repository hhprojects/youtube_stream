package com.youtubestream.app.ui.player

/** Which resting position the player sheet is in. */
enum class SheetAnchor { Collapsed, Expanded }

/**
 * Maps the sheet's current pixel [offset] to 0f (fully collapsed) … 1f (fully expanded).
 * [collapsedOffset] is the larger value (sheet pushed down); [expandedOffset] is usually 0.
 * Guards a zero span so a not-yet-measured sheet returns 0 instead of NaN.
 */
fun progressFor(offset: Float, collapsedOffset: Float, expandedOffset: Float): Float {
    val span = collapsedOffset - expandedOffset
    if (span == 0f) return 0f
    return ((collapsedOffset - offset) / span).coerceIn(0f, 1f)
}

/**
 * Decide whether a drag/fling released from the EXPANDED player should settle collapsed.
 *
 * [progress] is 0f (collapsed) … 1f (expanded) at the moment of release.
 * [velocityY] is the release velocity in px/s; positive = downward = toward collapse (this file's
 * convention — see PlayerSheet.onPostScroll, which starts the collapse on a positive available.y).
 *
 * A decisive flick wins outright; only a gentle (sub-threshold) release falls back to the distance
 * tiebreak — so "drag down a bit, then flick back up to cancel" correctly stays expanded.
 */
fun shouldCollapse(
    progress: Float,
    velocityY: Float,
    velocityThresholdPx: Float,
    positionalThreshold: Float,
): Boolean {
    if (velocityY > velocityThresholdPx) return true     // decisive down-flick → collapse
    if (velocityY < -velocityThresholdPx) return false   // decisive up-flick → stay expanded
    return progress < positionalThreshold                // gentle release → distance decides
}
