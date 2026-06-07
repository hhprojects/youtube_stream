package com.youtubestream.app.data.model

/**
 * Parse ytmusicapi's episode duration into seconds. Handles the two shapes seen in the wild:
 *  - word form: "1 hr 49 min", "23 min", "2 hr"
 *  - clock form: "1:49:00" (h:m:s) or "23:00" (m:s)
 * Anything unrecognized (incl. the unreliable view-count string, or null) → 0.
 */
fun parsePodcastDuration(raw: String?): Int {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return 0

    if (':' in s) {
        val parts = s.split(':').map { it.trim().toIntOrNull() ?: return 0 }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            else -> 0
        }
    }

    val hr = Regex("""(\d+)\s*hr""").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val min = Regex("""(\d+)\s*min""").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return if (hr == 0 && min == 0) 0 else hr * 3600 + min * 60
}
