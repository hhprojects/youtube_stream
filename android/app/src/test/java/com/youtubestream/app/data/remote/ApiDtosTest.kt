package com.youtubestream.app.data.remote

import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.SearchResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** DTOs must parse the REAL backend JSON (verified against server.js), not the spec's idealized shape. */
class ApiDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesSearchResponse() {
        // Mirrors /api/search: duration is a raw yt-dlp number.
        val body = """{"results":[{"id":"abc","title":"Song A","channel":"Chan","duration":210,"url":"https://youtu.be/abc","thumbnail":"http://t/a.jpg"}]}"""
        val parsed = json.decodeFromString<SearchResponseDto>(body)
        assertEquals(1, parsed.results.size)
        assertEquals("abc", parsed.results[0].id)
        assertEquals(210.0, parsed.results[0].duration!!, 0.0)
    }

    @Test
    fun searchResultToleratesMissingDuration() {
        val parsed = json.decodeFromString<SearchResponseDto>(
            """{"results":[{"id":"x","title":"t","channel":"c","url":"u","thumbnail":"th"}]}"""
        )
        assertEquals(null, parsed.results[0].duration)
    }

    @Test
    fun parsesLibraryResponse_durationIsStringUnknown() {
        // /api/library returns duration:'Unknown' (a STRING) and an ISO dateAdded.
        val body = """{"songs":[{"id":"s.m4a","title":"T","artist":"A","duration":"Unknown","filename":"s.m4a","downloadUrl":"http://pi/downloads/s.m4a","size":123,"dateAdded":"2026-06-01T00:00:00.000Z"}]}"""
        val parsed = json.decodeFromString<LibraryResponseDto>(body)
        assertEquals("Unknown", parsed.songs[0].duration)
        assertEquals(123L, parsed.songs[0].size)
    }

    @Test
    fun libraryResponseParsesThumbnail() {
        val body = """{"songs":[{"id":"s.m4a","title":"T","artist":"A","duration":"Unknown","filename":"s.m4a","downloadUrl":"http://pi/downloads/s.m4a","size":1,"dateAdded":"2026-06-01T00:00:00.000Z","thumbnail":"http://i/x.jpg"}]}"""
        val parsed = json.decodeFromString<LibraryResponseDto>(body)
        assertEquals("http://i/x.jpg", parsed.songs[0].thumbnail)
    }
}
