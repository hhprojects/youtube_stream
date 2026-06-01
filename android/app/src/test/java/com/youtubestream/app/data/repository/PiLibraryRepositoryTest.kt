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

class PiLibraryRepositoryTest {

    @Test
    fun mapsLibraryToPiSongs() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(
            MockResponse().setBody(
                """{"songs":[{"id":"s1","title":"T","artist":"A","duration":"Unknown","filename":"s1.m4a","downloadUrl":"http://pi/downloads/s1.m4a","size":99,"dateAdded":"2026-01-01T00:00:00.000Z"}]}"""
            )
        )
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(YoutubeStreamApi::class.java)

        val songs = PiLibraryRepository(api).piLibrary()

        assertEquals(1, songs.size)
        assertEquals("s1", songs[0].id)
        assertEquals(99L, songs[0].size)
        server.shutdown()
    }
}
