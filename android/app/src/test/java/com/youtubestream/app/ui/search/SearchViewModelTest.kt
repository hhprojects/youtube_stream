package com.youtubestream.app.ui.search

import app.cash.turbine.test
import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.remote.dto.SearchResponseDto
import com.youtubestream.app.data.remote.dto.SearchResultDto
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private fun fakeApi(onSearch: suspend (String) -> SearchResponseDto) = object : YoutubeStreamApi {
        override suspend fun search(body: SearchRequestDto) = onSearch(body.query)
        override suspend fun download(body: DownloadRequestDto) = error("unused")
        override suspend fun library(): LibraryResponseDto = error("unused")
        override suspend fun deleteFromPi(filename: String) = error("unused")
    }

    private class FakeDao(initial: List<LibrarySong> = emptyList()) : LibraryDao {
        val songs = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<LibrarySong>> = songs
        override suspend fun exists(id: String) = songs.value.any { it.id == id }
        override suspend fun insert(song: LibrarySong) = songs.update { it + song }
        override suspend fun deleteById(id: String) = songs.update { l -> l.filterNot { it.id == id } }
    }

    private fun vmWith(
        onSearch: suspend (String) -> SearchResponseDto = { error("search unused") },
        downloader: (String, String) -> Flow<DownloadState> = { _, _ -> emptyFlow() },
        dao: FakeDao = FakeDao(),
    ) = SearchViewModel(
        repo = SearchRepository(fakeApi(onSearch)),
        downloader = { id, title -> downloader(id, title) },
        library = LibraryRepository(dao),
    )

    // --- search (migrated from Plan 3) ---

    @Test fun emitsLoadingThenContent() = runTest {
        val vm = vmWith(onSearch = { SearchResponseDto(listOf(SearchResultDto("v1", "T", "C", 120.0, null, "th"))) })
        vm.state.test {
            assertEquals(UiState.Idle, awaitItem())
            vm.search("lofi")
            assertEquals(UiState.Loading, awaitItem())
            val content = awaitItem() as UiState.Content
            assertEquals(1, content.data.size)
            assertEquals("v1", content.data[0].id)
        }
    }

    @Test fun failureEmitsError() = runTest {
        val vm = vmWith(onSearch = { throw IOException("offline") })
        vm.state.test {
            awaitItem()            // Idle
            vm.search("x")
            awaitItem()            // Loading
            assertTrue(awaitItem() is UiState.Error)
        }
    }

    @Test fun blankQueryDoesNothing() = runTest {
        val vm = vmWith(onSearch = { error("should not be called") })
        vm.state.test {
            assertEquals(UiState.Idle, awaitItem())
            vm.search("   ")
            expectNoEvents()
        }
    }

    // --- download (new) ---

    @Test fun downloadTracksProgressThenClearsWhenComplete() = runTest {
        val channel = Channel<DownloadState>(Channel.UNLIMITED)
        val vm = vmWith(downloader = { _, _ -> channel.receiveAsFlow() })
        vm.download(SearchResult("v1", "Title", "Chan", null, null))
        runCurrent()

        channel.send(DownloadState.InProgress(0.5f))
        runCurrent()
        assertEquals(ItemDownload.Downloading(0.5f), vm.downloads.value["v1"])

        val song = LibrarySong("v1", "Title", "Artist", 0, "v1.m4a", "/songs/v1.m4a", 1L, 1L)
        channel.send(DownloadState.Completed(song))
        runCurrent()
        assertNull(vm.downloads.value["v1"])   // finished ids move out of the progress map

        channel.close()
    }

    @Test fun downloadFailureMarksRowFailed() = runTest {
        val channel = Channel<DownloadState>(Channel.UNLIMITED)
        val vm = vmWith(downloader = { _, _ -> channel.receiveAsFlow() })
        vm.download(SearchResult("v2", "T", "C", null, null))
        runCurrent()
        channel.send(DownloadState.Failed(RuntimeException("boom")))
        runCurrent()
        assertTrue(vm.downloads.value["v2"] is ItemDownload.Failed)
        channel.close()
    }

    @Test fun downloadedIdsReflectLibrary() = runTest {
        val dao = FakeDao(listOf(LibrarySong("v1", "T", "A", 0, "v1.m4a", "/songs/v1.m4a", 1L, 1L)))
        val vm = vmWith(dao = dao)
        vm.downloadedIds.test {
            assertEquals(emptySet<String>(), awaitItem())   // stateIn initial
            assertEquals(setOf("v1"), awaitItem())          // from the library flow
        }
    }
}
