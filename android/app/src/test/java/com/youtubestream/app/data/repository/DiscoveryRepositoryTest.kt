package com.youtubestream.app.data.repository

import com.youtubestream.app.data.remote.DiscoveryApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DiscoveryRepositoryTest {

    private fun apiFor(server: MockWebServer): DiscoveryApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DiscoveryApi::class.java)
    }

    @Test
    fun trendingMapsToDomain() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(
            MockResponse().setBody(
                """{"songs":[{"id":"v1","title":"T","artist":"A","thumbnail":"u","duration":"3:01"}]}"""
            )
        )
        val repo = DiscoveryRepository(apiFor(server))

        val songs = repo.trending("US")

        assertEquals(1, songs.size)
        assertEquals("v1", songs[0].videoId)
        assertEquals("A", songs[0].artist)
        assertEquals("u", songs[0].thumbnailUrl)
        server.shutdown()
    }

    @Test
    fun moodsMapsToDomain() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(
            MockResponse().setBody("""{"categories":[{"key":"k1","title":"Chill","section":"Moods"}]}""")
        )
        val repo = DiscoveryRepository(apiFor(server))

        val cats = repo.moods()

        assertEquals(1, cats.size)
        assertEquals("k1", cats[0].key)
        assertEquals("Chill", cats[0].title)
        server.shutdown()
    }

    @Test
    fun moodSongsMapsTitleAndSongs() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(
            MockResponse().setBody("""{"title":"Chill","songs":[{"id":"m1","title":"S","artist":"A"}]}""")
        )
        val repo = DiscoveryRepository(apiFor(server))

        val detail = repo.moodSongs("k1")

        assertEquals("Chill", detail.title)
        assertEquals(1, detail.songs.size)
        assertEquals("m1", detail.songs[0].videoId)
        server.shutdown()
    }

    @Test
    fun genreChartsMapsAndCleansTitle() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(MockResponse().setBody(
            """{"charts":[{"key":"PL1","title":"Top 50 Pop Music Videos United States"}]}"""))
        val repo = DiscoveryRepository(apiFor(server))
        val charts = repo.genreCharts("US")
        assertEquals(1, charts.size)
        assertEquals("PL1", charts[0].key)
        assertEquals("Pop", charts[0].title)   // cleaned
        server.shutdown()
    }

    @Test
    fun playlistSongsMapsTitleAndSongs() = runTest {
        val server = MockWebServer().apply { start() }
        server.enqueue(MockResponse().setBody(
            """{"title":"Top 50 Pop Music Videos United States","songs":[{"id":"v1","title":"S","artist":"A"}]}"""))
        val repo = DiscoveryRepository(apiFor(server))
        val detail = repo.playlistSongs("PL1")
        assertEquals(1, detail.songs.size)
        assertEquals("v1", detail.songs[0].videoId)
        server.shutdown()
    }
}
