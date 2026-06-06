package com.youtubestream.app.data.util

/**
 * Pulls the 11-char YouTube video id out of any common URL form (watch?v=, youtu.be/, music.,
 * /shorts/, /embed/), a bare id, or returns null if there isn't one. Pure logic — no Android
 * imports — so it unit-tests on the JVM. Feeds the artwork-edit feature.
 */
object YouTubeUrl {
    private const val ID = "[A-Za-z0-9_-]{11}"
    private val QUERY_V = Regex("[?&]v=($ID)")
    private val PATH_FORM = Regex("(?:youtu\\.be/|/shorts/|/embed/|/v/)($ID)")
    private val BARE_ID = Regex("^$ID$")

    fun extractVideoId(input: String): String? {
        val s = input.trim()
        if (s.isEmpty()) return null
        QUERY_V.find(s)?.let { return it.groupValues[1] }
        PATH_FORM.find(s)?.let { return it.groupValues[1] }
        if (BARE_ID.matches(s)) return s
        return null
    }
}
