package com.youtubestream.app.ui.discover

import app.cash.turbine.test
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.GenreChart
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
        val onGenreCharts: suspend (String) -> List<GenreChart> = { listOf(GenreChart("PL1", "Pop")) },
    ) : DiscoverySource {
        override suspend fun trending(region: String) = onTrending(region)
        override suspend fun related(seedVideoId: String) = onRelated(seedVideoId)
        override suspend fun moods() = onMoods()
        override suspend fun moodSongs(key: String) = MoodDetail("", emptyList())
        override suspend fun genreCharts(region: String) = onGenreCharts(region)
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
        // History keys on the filename (the library id) now; the song carries the videoId the seed resolves to.
        history: List<PlayEvent> = listOf(PlayEvent(songId = "song.m4a", playedAt = 1L)),
        library: List<LibrarySong> = listOf(
            LibrarySong("song.m4a", "T", "A", 0, "song.m4a", "", 0, 0, videoId = "dQw4w9WgXcQ"),
        ),
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

    @Test fun reachableLoadsGenreCharts() = runTest {
        vm().state.test {
            var item = awaitItem()
            while (item is DiscoverUiState.Loading) item = awaitItem()
            assertNotNull((item as DiscoverUiState.Content).content.genreCharts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun genreChartsDegradeIndependently() = runTest {
        val source = FakeSource(onGenreCharts = { throw RuntimeException("genres broke") })
        vm(source = source).state.test {
            var item = awaitItem()
            while (item is DiscoverUiState.Loading) item = awaitItem()
            val c = (item as DiscoverUiState.Content).content
            assertNull(c.genreCharts)        // degraded
            assertNotNull(c.trending)        // others survive
            cancelAndIgnoreRemainingEvents()
        }
    }
}
