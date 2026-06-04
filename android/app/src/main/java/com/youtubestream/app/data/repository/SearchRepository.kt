package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.remote.dto.SearchResultDto
import kotlinx.coroutines.CancellationException

class SearchRepository(private val api: YoutubeStreamApi) {
    /** Throws on IO/HTTP failure; the ViewModel layer maps that to an error UiState (Plan 3). */
    suspend fun search(query: String): List<SearchResult> =
        api.search(SearchRequestDto(query)).results.map { it.toDomain() }

    /**
     * Best-effort album art for an imported song, which arrives from the Pi with no thumbnail: search the
     * title and take the top hit's image. Never throws (cancellation aside) — a miss yields null, so the
     * import still succeeds and the row falls back to the placeholder. Imprecise by nature (the top hit can
     * be a cover/live version), the accepted trade-off for a backend-free fix.
     */
    suspend fun bestThumbnail(query: String): String? {
        if (query.isBlank()) return null
        return try {
            api.search(SearchRequestDto(query)).results.firstOrNull()?.thumbnail
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            null
        }
    }
}

private fun SearchResultDto.toDomain() = SearchResult(
    id = id,
    title = title,
    channel = channel,
    durationSeconds = duration?.toInt(),   // backend Double? -> domain Int?
    thumbnailUrl = thumbnail,
)
