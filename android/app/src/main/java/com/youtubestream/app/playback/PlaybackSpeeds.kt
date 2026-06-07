package com.youtubestream.app.playback

val PODCAST_SPEEDS = listOf(1f, 1.25f, 1.5f, 2f)

/** Cycle to the next speed; an unrecognized value snaps back to 1×. */
fun nextPlaybackSpeed(current: Float): Float {
    val i = PODCAST_SPEEDS.indexOfFirst { kotlin.math.abs(it - current) < 0.001f }
    return if (i < 0) 1f else PODCAST_SPEEDS[(i + 1) % PODCAST_SPEEDS.size]
}

/** "1×", "1.25×", "1.5×", "2×" — no trailing zeros. */
fun formatSpeed(speed: Float): String {
    val s = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')
    return "$s×"
}
