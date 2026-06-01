package com.youtubestream.app.ui.search

import app.cash.turbine.test
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.remote.dto.SearchResponseDto
import com.youtubestream.app.data.remote.dto.SearchResultDto
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    }

    private fun vmWith(onSearch: suspend (String) -> SearchResponseDto) =
        SearchViewModel(SearchRepository(fakeApi(onSearch)))

    @Test fun emitsLoadingThenContent() = runTest {
        val vm = vmWith { SearchResponseDto(listOf(SearchResultDto("v1", "T", "C", 120.0, null, "th"))) }
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
        val vm = vmWith { throw IOException("offline") }
        vm.state.test {
            awaitItem()            // Idle
            vm.search("x")
            awaitItem()            // Loading
            assertTrue(awaitItem() is UiState.Error)
        }
    }

    @Test fun blankQueryDoesNothing() = runTest {
        val vm = vmWith { error("should not be called") }
        vm.state.test {
            assertEquals(UiState.Idle, awaitItem())
            vm.search("   ")
            expectNoEvents()
        }
    }
}
