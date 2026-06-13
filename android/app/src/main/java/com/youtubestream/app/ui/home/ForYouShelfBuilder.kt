package com.youtubestream.app.ui.home

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent

/**
 * Pure ranking: (library, play-events, now) → ordered, non-empty shelves. Zero Android imports → JVM-tested.
 * Shelves below [MIN_VISIBLE] items are omitted, so a user with little or no play history may see no shelves.
 */
object ForYouShelfBuilder {
    private const val MAX_ITEMS = 12
    private const val MIN_VISIBLE = 4               // "On repeat" needs volume to be a meaningful signal
    private const val RECENT_MIN_VISIBLE = 2        // "Recently played" is a quick-resume row — surface it sooner
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val RECENT_WINDOW_MS = 30 * DAY_MS
    private const val ON_REPEAT_MIN = 3

    fun build(songs: List<LibrarySong>, events: List<PlayEvent>, now: Long): List<Shelf> {
        val byId = songs.associateBy { it.id }
        fun resolve(ids: List<String>) = ids.mapNotNull { byId[it] }.take(MAX_ITEMS)

        // Recently played: distinct songs, most recent play first.
        val recentlyPlayed = resolve(
            events.sortedByDescending { it.playedAt }.map { it.songId }.distinct(),
        )

        // On repeat: >= ON_REPEAT_MIN plays within the recent window, by count desc.
        val recentCounts = events.filter { it.playedAt >= now - RECENT_WINDOW_MS }
            .groupingBy { it.songId }.eachCount()
        val onRepeat = resolve(
            recentCounts.entries.filter { it.value >= ON_REPEAT_MIN }
                .sortedByDescending { it.value }.map { it.key },
        )

        return listOfNotNull(
            Shelf(ShelfId.RECENTLY_PLAYED, "Recently played", recentlyPlayed)
                .takeIf { it.songs.size >= RECENT_MIN_VISIBLE },
            Shelf(ShelfId.ON_REPEAT, "On repeat", onRepeat)
                .takeIf { it.songs.size >= MIN_VISIBLE },
        )
    }
}
