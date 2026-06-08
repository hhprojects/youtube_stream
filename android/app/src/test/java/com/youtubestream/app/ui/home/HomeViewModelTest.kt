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
        // 4 distinct recent plays → a visible "Recently played" shelf (proves history flows through).
        val history = MutableStateFlow(
            listOf(PlayEvent(0, "a", 1), PlayEvent(0, "b", 2), PlayEvent(0, "c", 3), PlayEvent(0, "d", 4)),
        )
        val vm = HomeViewModel(
            observeLibrary = { library },
            observeHistory = { history },
            play = { _, _ -> },
            clock = { 0L },
        )
        backgroundScope.launch { vm.state.collect {} }   // activate WhileSubscribed
        runCurrent()

        val shelves = (vm.state.value as UiState.Content).data
        assertEquals(listOf(ShelfId.RECENTLY_PLAYED), shelves.map { it.id })
        assertTrue(shelves.first().songs.isNotEmpty())
    }
}
