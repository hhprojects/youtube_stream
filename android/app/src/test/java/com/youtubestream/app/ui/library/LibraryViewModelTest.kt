package com.youtubestream.app.ui.library

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DeleteResponseDto
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.playback.PlayableTrack
import com.youtubestream.app.playback.PlaybackController
import com.youtubestream.app.playback.PlayerUiState
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private class FakeDao(initial: List<LibrarySong> = emptyList()) : LibraryDao {
        val songs = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<LibrarySong>> = songs
        override suspend fun exists(id: String) = songs.value.any { it.id == id }
        override suspend fun insert(song: LibrarySong) = songs.update { it + song }
        override suspend fun deleteById(id: String) = songs.update { l -> l.filterNot { it.id == id } }
    }

    private class FakeController : PlaybackController {
        override val state: StateFlow<PlayerUiState> = MutableStateFlow(PlayerUiState())
        var lastTracks: List<PlayableTrack>? = null
        var lastStartIndex: Int = -1
        override fun setQueueAndPlay(tracks: List<PlayableTrack>, startIndex: Int) {
            lastTracks = tracks; lastStartIndex = startIndex
        }
        override fun togglePlayPause() {}
        override fun next() {}
        override fun previous() {}
        override fun seekTo(positionMs: Long) {}
        override fun toggleShuffle() {}
        override fun cycleRepeat() {}
    }

    private fun song(id: String) = LibrarySong(id, "T$id", "A$id", 0, "$id.m4a", "/songs/$id.m4a", 1L, 1L)

    /** A Pi repo backed by a fake api; [onDelete] records/controls the DELETE call. */
    private fun piRepo(onDelete: suspend (String) -> DeleteResponseDto = { error("unused") }) =
        PiLibraryRepository(object : YoutubeStreamApi {
            override suspend fun search(body: SearchRequestDto) = error("unused")
            override suspend fun download(body: DownloadRequestDto) = error("unused")
            override suspend fun library() = error("unused")
            override suspend fun deleteFromPi(filename: String) = onDelete(filename)
        })

    @Test fun exposesLibraryAsContent() = runTest {
        val vm = LibraryViewModel(LibraryRepository(FakeDao(listOf(song("a"), song("b")))), piRepo(), FakeController())
        backgroundScope.launch { vm.state.collect {} }   // activate WhileSubscribed
        runCurrent()
        val content = vm.state.value as UiState.Content
        assertEquals(listOf("a", "b"), content.data.map { it.id })
    }

    @Test fun playSendsWholeLibraryAsQueue() = runTest {
        val controller = FakeController()
        val songs = listOf(song("a"), song("b"))
        val vm = LibraryViewModel(LibraryRepository(FakeDao(songs)), piRepo(), controller)
        vm.play(songs, startIndex = 1)
        assertEquals(2, controller.lastTracks?.size)
        assertEquals("a", controller.lastTracks?.get(0)?.mediaId)
        assertEquals("file:/songs/a.m4a", controller.lastTracks?.get(0)?.uri)
        assertEquals(1, controller.lastStartIndex)
    }

    @Test fun deleteRemovesFromLibrary() = runTest {
        val dao = FakeDao(listOf(song("a"), song("b")))
        val vm = LibraryViewModel(LibraryRepository(dao), piRepo(), FakeController())
        vm.delete(song("a"))
        runCurrent()
        assertEquals(listOf("b"), dao.songs.value.map { it.id })
    }

    @Test fun deleteEverywhereRemovesLocalAndPi() = runTest {
        val deleted = mutableListOf<String>()
        val dao = FakeDao(listOf(song("a"), song("b")))
        val pi = piRepo(onDelete = { deleted += it; DeleteResponseDto(true) })
        val vm = LibraryViewModel(LibraryRepository(dao), pi, FakeController())
        vm.deleteEverywhere(song("a"))
        runCurrent()
        assertEquals(listOf("a.m4a"), deleted)                    // Pi delete called with the filename
        assertEquals(listOf("b"), dao.songs.value.map { it.id })  // local row removed too
    }

    @Test fun deleteEverywhereKeepsLocalWhenPiFails() = runTest {
        val dao = FakeDao(listOf(song("a"), song("b")))
        val pi = piRepo(onDelete = { throw RuntimeException("Pi offline") })
        val vm = LibraryViewModel(LibraryRepository(dao), pi, FakeController())
        val errors = mutableListOf<String>()
        backgroundScope.launch { vm.errors.collect { errors += it } }
        runCurrent()                                              // subscribe before emitting
        vm.deleteEverywhere(song("a"))
        runCurrent()
        assertEquals(listOf("a", "b"), dao.songs.value.map { it.id })  // local UNTOUCHED — Pi failed first
        assertEquals(1, errors.size)                                   // a user-facing error was emitted
    }
}
