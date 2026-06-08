package com.youtubestream.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PodcastShowDto(
    val showId: String,
    val title: String,
    val author: String? = null,       // absent on search results; present from get_podcast
    val thumbnail: String? = null,
)

@Serializable
data class PodcastShelfDto(val label: String, val shows: List<PodcastShowDto> = emptyList())

@Serializable
data class PodcastHomeDto(val shelves: List<PodcastShelfDto> = emptyList())

@Serializable
data class PodcastSearchResultDto(val shows: List<PodcastShowDto> = emptyList())

@Serializable
data class PodcastEpisodeDto(
    val videoId: String,
    val title: String,
    val duration: String? = null,     // "1 hr 49 min" — parsed via parsePodcastDuration
    val date: String? = null,         // raw, display-only (unreliable)
    val description: String? = null,
    val thumbnail: String? = null,
)

@Serializable
data class PodcastShowDetailDto(
    val showId: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val episodes: List<PodcastEpisodeDto> = emptyList(),
)

@Serializable
data class PodcastDownloadRequestDto(
    val videoId: String,
    val title: String,
    val showName: String,
    val showId: String? = null,
    val date: String? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
)

@Serializable
data class PodcastDownloadResponseDto(
    val success: Boolean,
    val filename: String,
    val downloadUrl: String,
    val size: Long,
)

@Serializable
data class ShowIdsDto(val showIds: List<String>)

@Serializable
data class LatestShowDto(
    val showId: String,
    val title: String? = null,
    val episodes: List<PodcastEpisodeDto> = emptyList(),   // newest-first
)

@Serializable
data class PodcastLatestResponseDto(val shows: List<LatestShowDto> = emptyList())
