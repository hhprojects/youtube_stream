package com.youtubestream.app.data.repository

import com.youtubestream.app.data.remote.YoutubeStreamApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SearchRepositoryTest {

    private fun apiFor(server: MockWebServer): YoutubeStreamApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YoutubeStreamApi::class.java)
    }

    @Test
    fun mapsSearchResultsToDomain() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(
            MockResponse().setBody(
                """{"results":[{"id":"v1","title":"T","channel":"C","duration":120,"url":"https://y/v1","thumbnail":"u"}]}"""
            )
        )
        val repo = SearchRepository(apiFor(server))

        val results = repo.search("anything")

        assertEquals(1, results.size)
        assertEquals("v1", results[0].id)
        assertEquals(120, results[0].durationSeconds)  // Double 120.0 → Int 120
        assertEquals("u", results[0].thumbnailUrl)
        server.shutdown()
    }
}
