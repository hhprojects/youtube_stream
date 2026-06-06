package com.youtubestream.app.ui.home

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent

/**
 * Pure ranking: (library, play-events, now) → ordered, non-empty shelves. Zero Android imports → JVM-tested.
 * Shelves below [MIN_VISIBLE] items are omitted, so a no-history user sees only "Recently added".
 */
object ForYouShelfBuilder {
    private const val MAX_ITEMS = 12
    private const val MIN_VISIBLE = 4
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val RECENT_WINDOW_MS = 30 * DAY_MS
    private const val ON_REPEAT_MIN = 3
    private const val REDISCOVER_STALE_MS = 30 * DAY_MS

    fun build(songs: List<LibrarySong>, events: List<PlayEvent>, now: Long): List<Shelf> {
        val byId = songs.associateBy { it.id }
        fun resolve(ids: List<String>) = ids.mapNotNull { byId[it] }.take(MAX_ITEMS)

        // Recently played: distinct songs, most recent play first.
        val recentlyPlayed = resolve(
            events.sortedByDescending { it.playedAt }.map { it.songId }.distinct(),
        )

        // Most played: count all-time, desc; tie-break by most recent play.
        val lastPlayed = events.groupBy { it.songId }.mapValues { (_, e) -> e.maxOf { it.playedAt } }
        val counts = events.groupingBy { it.songId }.eachCount()
        val mostPlayed = resolve(
            counts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { lastPlayed[it.key] ?: 0L })
                .map { it.key },
        )

        // On repeat: >= ON_REPEAT_MIN plays within the recent window, by count desc.
        val recentCounts = events.filter { it.playedAt >= now - RECENT_WINDOW_MS }
            .groupingBy { it.songId }.eachCount()
        val onRepeat = resolve(
            recentCounts.entries.filter { it.value >= ON_REPEAT_MIN }
                .sortedByDescending { it.value }.map { it.key },
        )

        // Rediscover: played before but not within the stale window, by all-time count desc.
        val rediscover = resolve(
            counts.keys
                .filter { (lastPlayed[it] ?: Long.MAX_VALUE) < now - REDISCOVER_STALE_MS }
                .sortedByDescending { counts[it] ?: 0 },
        )

        // Recently added: by dateAdded desc (don't assume input order).
        val recentlyAdded = songs.sortedByDescending { it.dateAdded }.take(MAX_ITEMS)

        return listOf(
            Shelf(ShelfId.RECENTLY_PLAYED, "Recently played", recentlyPlayed),
            Shelf(ShelfId.ON_REPEAT, "On repeat", onRepeat),
            Shelf(ShelfId.MOST_PLAYED, "Most played", mostPlayed),
            Shelf(ShelfId.REDISCOVER, "Rediscover", rediscover),
            Shelf(ShelfId.RECENTLY_ADDED, "Recently added", recentlyAdded),
        ).filter { it.songs.size >= MIN_VISIBLE }
    }
}
