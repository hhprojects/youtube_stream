package com.youtubestream.app.ui.discover

import app.cash.turbine.test
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodCategory
import com.youtubestream.app.data.model.MoodDetail
import com.youtubestream.app.data.network.ReachabilitySource
import com.youtubestream.app.data.network.ServerStatus
import com.youtubestream.app.data.repository.DiscoverySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiscoverViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private class FakeSource(
        val onTrending: suspend (String) -> List<DiscoverySong> = { listOf(DiscoverySong("t1", "T", "A", null)) },
        val onRelated: suspend (String) -> List<DiscoverySong> = { listOf(DiscoverySong("r1", "T", "A", null)) },
        val onMoods: suspend () -> List<MoodCategory> = { listOf(MoodCategory("k", "Chill", "Moods")) },
    ) : DiscoverySource {
        override suspend fun trending(region: String) = onTrending(region)
        override suspend fun related(seedVideoId: String) = onRelated(seedVideoId)
        override suspend fun moods() = onMoods()
        override suspend fun moodSongs(key: String) = MoodDetail("", emptyList())
        override suspend fun genreCharts(region: String) = emptyList<com.youtubestream.app.data.model.GenreChart>()
        override suspend fun playlistSongs(playlistId: String) = MoodDetail("", emptyList())
    }

    private class FakeReach(initial: ServerStatus) : ReachabilitySource {
        val flow = MutableStateFlow(initial)
        override val status: StateFlow<ServerStatus> = flow
        override suspend fun probe() {}
    }

    private fun vm(
        source: DiscoverySource = FakeSource(),
        reach: FakeReach = FakeReach(ServerStatus.REACHABLE),
        history: List<PlayEvent> = listOf(PlayEvent(songId = "dQw4w9WgXcQ", playedAt = 1L)),
        library: List<LibrarySong> = emptyList(),
    ) = DiscoverViewModel(
        source = source,
        reachability = reach,
        observeLibrary = { MutableStateFlow(library) },
        observeHistory = { MutableStateFlow(history) },
        downloads = DiscoveryDownloads(downloader = { _, _, _ -> emptyFlow() }, play = {}),
        region = { "US" },
    )

    @Test fun reachableLoadsAllShelves() = runTest {
        vm().state.test {
            var item = awaitItem()
            while (item is DiscoverUiState.Loading) item = awaitItem()
            val c = (item as DiscoverUiState.Content).content
            assertNotNull(c.trending)
            assertNotNull(c.related)
            assertNotNull(c.moods)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun perShelfDegrade_trendingFailureNullsOnlyTrending() = runTest {
        val source = FakeSource(onTrending = { throw RuntimeException("ytmusicapi broke") })
        vm(source = source).state.test {
            var item = awaitItem()
            while (item is DiscoverUiState.Loading) item = awaitItem()
            val c = (item as DiscoverUiState.Content).content
            assertNull(c.trending)
            assertNotNull(c.related)
            assertNotNull(c.moods)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun noSeedNullsRelated() = runTest {
        vm(history = emptyList()).state.test {
            var item = awaitItem()
            while (item is DiscoverUiState.Loading) item = awaitItem()
            val c = (item as DiscoverUiState.Content).content
            assertNull(c.related)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun unreachableIsHidden() = runTest {
        vm(reach = FakeReach(ServerStatus.SERVER_UNREACHABLE)).state.test {
            var item = awaitItem()
            while (item is DiscoverUiState.Loading) item = awaitItem()
            assertTrue(item is DiscoverUiState.Hidden)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
