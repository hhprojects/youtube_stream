package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

class DownloadRepositoryTest {

    private class FakeDao : LibraryDao {
        val songs = MutableStateFlow(emptyList<LibrarySong>())
        override fun observeAll(): Flow<List<LibrarySong>> = songs
        override suspend fun exists(id: String) = songs.value.any { it.id == id }
        override suspend fun insert(song: LibrarySong) = songs.update { it + song }
        override suspend fun deleteById(id: String) = songs.update { l -> l.filterNot { it.id == id } }
        override suspend fun clearAllArtwork() = songs.update { list -> list.map { it.copy(artworkUrl = null) } }
    }

    @Test
    fun downloadsFileEmitsProgressAndInsertsRow() = runTest {
        val server = MockWebServer().apply { start() }
        val fileUrl = server.url("/downloads/v1.m4a").toString()   // backend returns ABSOLUTE urls
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"filename":"v1.m4a","downloadUrl":"$fileUrl","title":"T","artist":"A","size":4}"""
            )
        )
        server.enqueue(MockResponse().setBody("DATA"))

        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(YoutubeStreamApi::class.java)
        val dao = FakeDao()
        val songsDir = File.createTempFile("songs", "").let { it.delete(); it.mkdirs(); it }
        val repo = DownloadRepository(api, OkHttpClient(), dao, songsDir) { server.url("/").toString() }

        val states = repo.download("v1", "T", "http://img/v1.jpg").toList()

        assertTrue(states.any { it is DownloadState.InProgress })   // progress emitted
        val done = states.last() as DownloadState.Completed
        assertEquals("v1.m4a", done.song.id)                        // canonical id = filename
        assertEquals("v1", done.song.videoId)                       // YouTube id retained for match/seed
        assertEquals("http://img/v1.jpg", done.song.artworkUrl)     // thumbnail persisted onto the row
        assertTrue(File(songsDir, "v1.m4a").exists())               // file written
        assertEquals(listOf(done.song), dao.songs.value)           // row inserted
        server.shutdown()
    }

    @Test
    fun importStreamsPiFileAndInsertsRow() = runTest {
        val server = MockWebServer().apply { start() }
        val fileUrl = server.url("/downloads/s1.m4a").toString()   // Pi returns ABSOLUTE urls
        server.enqueue(MockResponse().setBody("DATA"))             // just the file — no POST for an import

        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(YoutubeStreamApi::class.java)
        val dao = FakeDao()
        val songsDir = File.createTempFile("songs", "").let { it.delete(); it.mkdirs(); it }
        val repo = DownloadRepository(api, OkHttpClient(), dao, songsDir) { server.url("/").toString() }

        val piSong = PiSong("s1", "T", "A", "s1.m4a", fileUrl, 4L)
        val states = repo.importSong(piSong).toList()

        assertTrue(states.any { it is DownloadState.InProgress })   // progress emitted
        val done = states.last() as DownloadState.Completed
        assertEquals("s1.m4a", done.song.id)                        // canonical id = filename
        assertTrue(File(songsDir, "s1.m4a").exists())               // file written
        assertEquals(listOf(done.song), dao.songs.value)           // row inserted (no POST hit)
        server.shutdown()
    }

    @Test
    fun importUsesPiThumbnail() = runTest {
        val server = MockWebServer().apply { start() }
        val fileUrl = server.url("/downloads/s1.m4a").toString()
        server.enqueue(MockResponse().setBody("DATA"))

        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(YoutubeStreamApi::class.java)
        val dao = FakeDao()
        val songsDir = File.createTempFile("songs", "").let { it.delete(); it.mkdirs(); it }
        val repo = DownloadRepository(api, OkHttpClient(), dao, songsDir) { server.url("/").toString() }

        val piSong = PiSong("s1", "T", "A", "s1.m4a", fileUrl, 4L, thumbnailUrl = "http://i/s1.jpg")
        val done = repo.importSong(piSong).toList().last() as DownloadState.Completed

        assertEquals("http://i/s1.jpg", done.song.artworkUrl)   // taken straight from the Pi, no guess
        server.shutdown()
    }
}
