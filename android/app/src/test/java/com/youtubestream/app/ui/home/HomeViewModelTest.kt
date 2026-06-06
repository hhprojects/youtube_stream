package com.youtubestream.app.ui.home

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private fun song(id: String) = LibrarySong(id, "T", "A", 1, "$id.m4a", "/p/$id.m4a", 1L, 0L)

    @Test fun emitsForYouShelvesFromLibraryAndHistory() = runTest {
        val library = MutableStateFlow(listOf(song("a"), song("b"), song("c"), song("d")))
        val history = MutableStateFlow(emptyList<PlayEvent>())
        val vm = HomeViewModel(
            observeLibrary = { library },
            observeHistory = { history },
            play = { _, _ -> },
            clock = { 0L },
        )
        backgroundScope.launch { vm.state.collect {} }   // activate WhileSubscribed
        runCurrent()

        val shelves = (vm.state.value as UiState.Content).data
        assertEquals(listOf(ShelfId.RECENTLY_ADDED), shelves.map { it.id })  // no history → only Recently added
        assertTrue(shelves.first().songs.isNotEmpty())
    }
}
