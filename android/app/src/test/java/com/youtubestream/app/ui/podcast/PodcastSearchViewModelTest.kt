package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.FollowedShow
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastFreshShelf
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.model.ShowNewEpisodes
import com.youtubestream.app.data.repository.PodcastDownloadState
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.download.DownloadQueue
import com.youtubestream.app.ui.download.PodcastDownloads
import com.youtubestream.app.ui.podcast.LatestEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks the two-section search contract: Shows and Videos load and fail independently, a video tap
 * routes through videoAsEpisode into the episode download queue, and downloadedVideoIds mirrors the
 * local store. The fake [PodcastSource] is the seam (same pattern as DownloadedEpisodesViewModelTest).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PodcastSearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val show = PodcastShowCard(showId = "MPSP1", title = "Real Show", author = null, artworkUrl = null)
    private val video = SearchResult(id = "vid1", title = "Great Talk", channel = "Conf Channel", durationSeconds = 60, thumbnailUrl = "t")

    private fun vm(
        fake: FakeSource,
        videoSearch: suspend (String) -> List<SearchResult> = { emptyList() },
    ) = PodcastSearchViewModel(fake, videoSearch, PodcastDownloads(DownloadQueue(CoroutineScope(dispatcher)), fake))

    @Test fun `search fills both sections independently`() = runTest(dispatcher) {
        val vm = vm(FakeSource(shows = listOf(show)), videoSearch = { listOf(video) })
        vm.search("kotlin")
        advanceUntilIdle()
        assertEquals(UiState.Content(listOf(show)), vm.shows.value)
        assertEquals(UiState.Content(listOf(video)), vm.videos.value)
    }

    @Test fun `a failing shows source degrades only the Shows section`() = runTest(dispatcher) {
        val vm = vm(FakeSource(searchError = "pi down"), videoSearch = { listOf(video) })
        vm.search("kotlin")
        advanceUntilIdle()
        assertEquals(UiState.Error("pi down"), vm.shows.value)
        assertEquals(UiState.Content(listOf(video)), vm.videos.value)
    }

    @Test fun `a failing videos source degrades only the Videos section`() = runTest(dispatcher) {
        val vm = vm(FakeSource(shows = listOf(show)), videoSearch = { error("yt down") })
        vm.search("kotlin")
        advanceUntilIdle()
        assertEquals(UiState.Content(listOf(show)), vm.shows.value)
        assertEquals(UiState.Error("yt down"), vm.videos.value)
    }

    @Test fun `a blank query resets both sections to Idle`() = runTest(dispatcher) {
        val vm = vm(FakeSource(shows = listOf(show)))
        vm.search("kotlin")
        advanceUntilIdle()
        vm.search("   ")
        assertEquals(UiState.Idle, vm.shows.value)
        assertEquals(UiState.Idle, vm.videos.value)
    }

    @Test fun `video tap enqueues an episode download grouped under the channel`() = runTest(dispatcher) {
        val fake = FakeSource()
        val vm = vm(fake)
        vm.onVideoTap(video)
        advanceUntilIdle()
        val (dlShow, dlEpisode) = fake.downloadCalls.single()
        assertEquals("video:vid1", dlShow.showId)
        assertEquals("Conf Channel", dlShow.title)
        assertEquals("vid1", dlEpisode.videoId)
        assertEquals("Great Talk", dlEpisode.title)
    }

    @Test fun `downloadedVideoIds mirrors the downloaded-episodes store`() = runTest(dispatcher) {
        val fake = FakeSource()
        val vm = vm(fake)
        val job = launch { vm.downloadedVideoIds.collect { } }   // WhileSubscribed needs a collector
        fake.downloaded.value = listOf(ep("f1", videoId = "vid1"), ep("f2", videoId = "vid2"))
        advanceUntilIdle()
        assertEquals(setOf("vid1", "vid2"), vm.downloadedVideoIds.value)
        job.cancel()
    }

    private fun ep(id: String, videoId: String) = PodcastEpisode(
        id = id, videoId = videoId, title = "T$id", showName = "S$id", showId = null,
        durationSeconds = 0, filename = id, localPath = "/tmp/$id", size = 0, dateAdded = 0,
    )
}

/** Minimal seam: records download calls; everything the VM doesn't touch errors loudly. */
private class FakeSource(
    private val shows: List<PodcastShowCard> = emptyList(),
    private val searchError: String? = null,
) : PodcastSource {
    val downloaded = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val downloadCalls = mutableListOf<Pair<PodcastShowDetail, PodcastEpisodeItem>>()

    override suspend fun search(query: String): List<PodcastShowCard> {
        searchError?.let { error(it) }
        return shows
    }

    override fun observeDownloadedEpisodes(): Flow<List<PodcastEpisode>> = downloaded

    override fun downloadEpisode(show: PodcastShowDetail, episode: PodcastEpisodeItem): Flow<PodcastDownloadState> {
        downloadCalls += show to episode
        return flowOf(PodcastDownloadState.InProgress(1f))
    }

    // --- unused by these tests ---
    override suspend fun home(): List<PodcastShelf> = error("unused")
    override suspend fun show(showId: String): PodcastShowDetail = error("unused")
    override fun observeIsFollowing(showId: String): Flow<Boolean> = error("unused")
    override suspend fun follow(detail: PodcastShowDetail) = error("unused")
    override suspend fun unfollow(showId: String) = error("unused")
    override suspend fun updateResumePosition(id: String, positionMs: Long, playedAt: Long) = error("unused")
    override suspend fun markFinished(id: String) = error("unused")
    override suspend fun isEpisode(mediaId: String): Boolean = error("unused")
    override suspend fun getEpisode(id: String): PodcastEpisode? = error("unused")
    override fun observeContinueListening(): Flow<List<PodcastEpisode>> = error("unused")
    override fun observeFollowedShows(): Flow<List<FollowedShow>> = error("unused")
    override suspend fun followedShowIds(): List<String> = error("unused")
    override suspend fun latestFromShows(showIds: List<String>): List<LatestEpisode> = error("unused")
    override suspend fun fresh(): List<PodcastFreshShelf> = error("unused")
    override suspend fun checkForNewEpisodes(): List<ShowNewEpisodes> = error("unused")
    override suspend fun deleteEpisode(episode: PodcastEpisode) = error("unused")
    override suspend fun deleteEpisodeEverywhere(episode: PodcastEpisode) = error("unused")
}
