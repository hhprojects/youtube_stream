package com.youtubestream.app.data.model

data class PodcastShowCard(val showId: String, val title: String, val author: String?, val artworkUrl: String?)

data class PodcastShelf(val label: String, val shows: List<PodcastShowCard>)

data class PodcastEpisodeItem(
    val videoId: String,
    val title: String,
    val durationSeconds: Int,
    val publishedDate: String?,
    val description: String?,
    val artworkUrl: String?,
)

data class PodcastShowDetail(
    val showId: String,
    val title: String,
    val author: String?,
    val description: String?,
    val artworkUrl: String?,
    val episodes: List<PodcastEpisodeItem>,   // newest-first, as returned
)
