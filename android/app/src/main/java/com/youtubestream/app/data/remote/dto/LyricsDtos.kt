package com.youtubestream.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LyricsDto(
    val synced: String? = null,
    val plain: String? = null,
    val source: String? = null,
)
