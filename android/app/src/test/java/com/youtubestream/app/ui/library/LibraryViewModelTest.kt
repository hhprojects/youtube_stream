package com.youtubestream.app.ui.library

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.repository.LibraryRepository
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

    @Test fun exposesLibraryAsContent() = runTest {
        val vm = LibraryViewModel(LibraryRepository(FakeDao(listOf(song("a"), song("b")))), FakeController())
        backgroundScope.launch { vm.state.collect {} }   // activate WhileSubscribed
        runCurrent()
        val content = vm.state.value as UiState.Content
        assertEquals(listOf("a", "b"), content.data.map { it.id })
    }

    @Test fun playSendsWholeLibraryAsQueue() = runTest {
        val controller = FakeController()
        val songs = listOf(song("a"), song("b"))
        val vm = LibraryViewModel(LibraryRepository(FakeDao(songs)), controller)
        vm.play(songs, startIndex = 1)
        assertEquals(2, controller.lastTracks?.size)
        assertEquals("a", controller.lastTracks?.get(0)?.mediaId)
        assertEquals("file:/songs/a.m4a", controller.lastTracks?.get(0)?.uri)
        assertEquals(1, controller.lastStartIndex)
    }

    @Test fun deleteRemovesFromLibrary() = runTest {
        val dao = FakeDao(listOf(song("a"), song("b")))
        val vm = LibraryViewModel(LibraryRepository(dao), FakeController())
        vm.delete(song("a"))
        runCurrent()
        assertEquals(listOf("b"), dao.songs.value.map { it.id })
    }
}
