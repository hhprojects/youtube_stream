package com.youtubestream.app.data.model

/** Clean domain model the UI consumes — backend DTO quirks are mapped away in the repository. */
data class SearchResult(
    val id: String,
    val title: String,
    val channel: String,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
)
