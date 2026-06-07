package com.youtubestream.app.data.model

/** A discovered (not-yet-local) song. Tapping it triggers the two-step download. */
data class DiscoverySong(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
)

/** A mood/genre category chip; [key] is the opaque ytmusicapi params token. */
data class MoodCategory(val key: String, val title: String, val section: String)

/** The songs of one mood/genre category, for the mood-detail screen. */
data class MoodDetail(val title: String, val songs: List<DiscoverySong>)
