package com.youtubestream.app.ui.player

/** Millis → "m:ss" (or "h:mm:ss" past an hour). Negative/unknown clamps to "0:00". */
fun formatTime(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
