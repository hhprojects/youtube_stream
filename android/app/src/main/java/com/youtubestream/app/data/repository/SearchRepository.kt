package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.remote.dto.SearchResultDto

class SearchRepository(private val api: YoutubeStreamApi) {
    /** Throws on IO/HTTP failure; the ViewModel layer maps that to an error UiState (Plan 3). */
    suspend fun search(query: String): List<SearchResult> =
        api.search(SearchRequestDto(query)).results.map { it.toDomain() }
}

private fun SearchResultDto.toDomain() = SearchResult(
    id = id,
    title = title,
    channel = channel,
    durationSeconds = duration?.toInt(),   // backend Double? -> domain Int?
    thumbnailUrl = thumbnail,
)
