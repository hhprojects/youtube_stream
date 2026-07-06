package com.youtubestream.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequestDto(val query: String)

@Serializable
data class SearchResponseDto(val results: List<SearchResultDto>)

@Serializable
data class SearchResultDto(
    val id: String,
    val title: String,
    val channel: String,
    val duration: Double? = null,   // raw yt-dlp seconds; may be absent or fractional
    val url: String? = null,
    val thumbnail: String? = null,
)

@Serializable
data class DownloadRequestDto(val videoId: String, val title: String)

@Serializable
data class DownloadResponseDto(
    val success: Boolean,
    val filename: String,
    val downloadUrl: String,        // absolute: http://<server>/downloads/<file>
    val title: String,
    val artist: String,
    val size: Long,
)

@Serializable
data class DeleteResponseDto(val success: Boolean)

@Serializable
data class LibraryResponseDto(val songs: List<LibrarySongDto>)

@Serializable
data class LibrarySongDto(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,           // backend sends the string 'Unknown', not a number
    val filename: String,
    val downloadUrl: String,        // absolute
    val size: Long,
    val dateAdded: String,          // ISO date string (Express-serialized Date)
    val thumbnail: String? = null,  // ytimg URL derived from the Pi sidecar; null when unknown
    val videoId: String? = null,    // YouTube id from the Pi sidecar; null for Pi-only files
)

@Serializable
data class ArtworkRequestDto(val videoId: String)

@Serializable
data class ArtworkResponseDto(val success: Boolean, val thumbnail: String? = null)

@Serializable
data class TitleRequestDto(val title: String)

@Serializable
data class TitleResponseDto(val success: Boolean, val title: String? = null)
