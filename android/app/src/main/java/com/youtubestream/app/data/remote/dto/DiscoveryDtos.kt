package com.youtubestream.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoverySongDto(
    val id: String,
    val title: String,
    val artist: String = "Unknown",
    val thumbnail: String? = null,
    val duration: String? = null,   // "m:ss" or null
)

@Serializable
data class DiscoveryShelfDto(val songs: List<DiscoverySongDto> = emptyList())

@Serializable
data class MoodCategoryDto(val key: String, val title: String, val section: String = "")

@Serializable
data class MoodsResponseDto(val categories: List<MoodCategoryDto> = emptyList())

@Serializable
data class MoodDetailDto(val title: String = "", val songs: List<DiscoverySongDto> = emptyList())

@Serializable
data class GenreChartDto(val key: String, val title: String)

@Serializable
data class GenreChartsResponseDto(val charts: List<GenreChartDto> = emptyList())
