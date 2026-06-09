package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.FollowedShow
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastFreshShelf
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildPodcastHomeTest {
    private fun ep(id: String) = PodcastEpisode(
        id = id, videoId = id, title = "E", showName = "S", showId = "sh", durationSeconds = 1,
        filename = id, localPath = "/p", size = 1, dateAdded = 0, resumePositionMs = 5,
    )
    private fun latest(id: String) = LatestEpisode("sh", "S", PodcastEpisodeItem(id, "E", 1, null, null, null))
    private fun shelf(label: String, n: Int) =
        PodcastShelf(label, List(n) { PodcastShowCard("s$it", "Show $it", null, null) })
    private fun followed(id: String) = FollowedShow(showId = id, title = "Show $id", author = "A", dateFollowed = 0)
    private fun fresh(label: String, n: Int) = PodcastFreshShelf(label, List(n) { latest("$label$it") })

    @Test fun `popular mode renders chrome, toggle, then show shelves only`() {
        val out = buildPodcastHome(
            PodcastHomeMode.Popular,
            followed = listOf(followed("x")), continueListening = listOf(ep("a")),
            popularShelves = listOf(shelf("AI", 2)),
            latest = listOf(latest("b")), freshTopics = listOf(fresh("AI", 1)),
        )
        assertTrue(out[0] is PodcastHomeSection.ContinueListening)
        assertTrue(out[1] is PodcastHomeSection.Following)
        assertEquals(PodcastHomeMode.Popular, (out[2] as PodcastHomeSection.ModeToggle).mode)
        assertTrue(out[3] is PodcastHomeSection.ShowShelf)
        assertEquals(4, out.size)
    }

    @Test fun `newest mode renders chrome, toggle, latest, then fresh shelves`() {
        val out = buildPodcastHome(
            PodcastHomeMode.Newest,
            followed = listOf(followed("x")), continueListening = listOf(ep("a")),
            popularShelves = listOf(shelf("AI", 2)),
            latest = listOf(latest("b")), freshTopics = listOf(fresh("AI", 1)),
        )
        assertTrue(out[0] is PodcastHomeSection.ContinueListening)
        assertTrue(out[1] is PodcastHomeSection.Following)
        assertEquals(PodcastHomeMode.Newest, (out[2] as PodcastHomeSection.ModeToggle).mode)
        assertTrue(out[3] is PodcastHomeSection.Latest)
        assertEquals("AI", (out[4] as PodcastHomeSection.FreshTopic).label)
        assertEquals(5, out.size)
    }

    @Test fun `drops empty chrome and empty popular shelves, toggle always present`() {
        val out = buildPodcastHome(
            PodcastHomeMode.Popular,
            followed = emptyList(), continueListening = emptyList(),
            popularShelves = listOf(shelf("Empty", 0), shelf("AI", 2)),
            latest = emptyList(), freshTopics = emptyList(),
        )
        assertTrue(out[0] is PodcastHomeSection.ModeToggle)
        assertEquals("AI", (out[1] as PodcastHomeSection.ShowShelf).label)
        assertEquals(2, out.size)
    }

    @Test fun `newest drops empty latest and empty fresh shelves`() {
        val out = buildPodcastHome(
            PodcastHomeMode.Newest,
            followed = emptyList(), continueListening = emptyList(),
            popularShelves = emptyList(),
            latest = emptyList(), freshTopics = listOf(fresh("Empty", 0), fresh("AI", 1)),
        )
        assertTrue(out[0] is PodcastHomeSection.ModeToggle)
        assertEquals("AI", (out[1] as PodcastHomeSection.FreshTopic).label)
        assertEquals(2, out.size)
    }

    @Test fun `newest with fresh empty still shows latest`() {
        val out = buildPodcastHome(
            PodcastHomeMode.Newest,
            followed = emptyList(), continueListening = emptyList(),
            popularShelves = emptyList(),
            latest = listOf(latest("ep1")), freshTopics = emptyList(),
        )
        assertTrue(out[0] is PodcastHomeSection.ModeToggle)
        assertTrue(out[1] is PodcastHomeSection.Latest)
        assertEquals(2, out.size)
    }
}
