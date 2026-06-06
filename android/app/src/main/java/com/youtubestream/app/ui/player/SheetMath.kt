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
