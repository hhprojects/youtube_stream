package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.FollowedShow
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastFreshShelf
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.model.ShowNewEpisodes
import com.youtubestream.app.data.repository.PodcastDownloadState
import com.youtubestream.app.data.repository.PodcastSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks the one piece of genuinely-new logic in [DownloadedEpisodesViewModel]: bulk delete over the
 * current selection — local fan-out, and the partial-failure report when some Pi deletes fail. The
 * fake [PodcastSource] is the seam. (Selection itself lives in the already-tested SelectionState;
 * the rest of the VM is Flow glue verified by build + on-device.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadedEpisodesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `deleteSelectedDownloads deletes only the selected episodes locally and exits selection`() =
        runTest(dispatcher) {
            val fake = FakePodcastSource()
            val vm = DownloadedEpisodesViewModel(fake, play = {})
            val all = listOf(ep("a"), ep("b"), ep("c"))

            vm.enterSelection()
            vm.toggle("a")
            vm.toggle("c")
            vm.deleteSelectedDownloads(all)
            advanceUntilIdle()

            assertEquals(setOf("a", "c"), fake.deletedLocal.toSet())
            assertTrue(fake.everywhereAttempts.isEmpty())
            assertFalse(vm.selection.value.active)
            assertTrue(vm.selection.value.selectedIds.isEmpty())
        }

    @Test fun `deleteSelectedEverywhere attempts every target and reports partial failure`() =
        runTest(dispatcher) {
            val fake = FakePodcastSource(failEverywhereFor = setOf("b"))
            val vm = DownloadedEpisodesViewModel(fake, play = {})
            val all = listOf(ep("a"), ep("b"), ep("c"))
            val seen = mutableListOf<String>()
            backgroundScope.launch { vm.errors.toList(seen) }

            vm.enterSelection()
            vm.toggleSelectAll(all.map { it.id })
            vm.deleteSelectedEverywhere(all)
            advanceUntilIdle()

            assertEquals(setOf("a", "b", "c"), fake.everywhereAttempts.toSet())
            assertEquals(listOf("2 of 3 removed from the server"), seen)
            assertFalse(vm.selection.value.active)
        }

    @Test fun `deleteSelectedEverywhere emits no error when all succeed`() =
        runTest(dispatcher) {
            val fake = FakePodcastSource()
            val vm = DownloadedEpisodesViewModel(fake, play = {})
            val all = listOf(ep("a"), ep("b"))
            val seen = mutableListOf<String>()
            backgroundScope.launch { vm.errors.toList(seen) }

            vm.enterSelection()
            vm.toggleSelectAll(all.map { it.id })
            vm.deleteSelectedEverywhere(all)
            advanceUntilIdle()

            assertEquals(setOf("a", "b"), fake.everywhereAttempts.toSet())
            assertTrue(seen.isEmpty())
            assertFalse(vm.selection.value.active)
        }

    private fun ep(id: String) = PodcastEpisode(
        id = id, videoId = "v$id", title = "T$id", showName = "S$id", showId = null,
        durationSeconds = 0, filename = id, localPath = "/tmp/$id", size = 0, dateAdded = 0,
    )
}

/** Records local + everywhere delete calls; throws on the everywhere path for designated ids (Pi failure). */
private class FakePodcastSource(
    private val failEverywhereFor: Set<String> = emptySet(),
) : PodcastSource {
    val deletedLocal = mutableListOf<String>()
    val everywhereAttempts = mutableListOf<String>()
    private val downloaded = MutableStateFlow<List<PodcastEpisode>>(emptyList())

    override fun observeDownloadedEpisodes(): Flow<List<PodcastEpisode>> = downloaded
    override suspend fun deleteEpisode(episode: PodcastEpisode) { deletedLocal += episode.id }
    override suspend fun deleteEpisodeEverywhere(episode: PodcastEpisode) {
        everywhereAttempts += episode.id
        if (episode.id in failEverywhereFor) error("pi down")
    }

    // --- unused by these tests ---
    override suspend fun home(): List<PodcastShelf> = error("unused")
    override suspend fun show(showId: String): PodcastShowDetail = error("unused")
    override fun observeIsFollowing(showId: String): Flow<Boolean> = error("unused")
    override suspend fun follow(detail: PodcastShowDetail) = error("unused")
    override suspend fun unfollow(showId: String) = error("unused")
    override fun downloadEpisode(show: PodcastShowDetail, episode: PodcastEpisodeItem): Flow<PodcastDownloadState> = error("unused")
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
    override suspend fun search(query: String): List<PodcastShowCard> = error("unused")
}
