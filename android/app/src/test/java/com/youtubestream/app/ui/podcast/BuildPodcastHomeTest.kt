package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.FollowedShow
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
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

    @Test fun `puts followed shows first, before continue, latest and shelves`() {
        val out = buildPodcastHome(
            listOf(followed("x"), followed("y")), listOf(ep("a")), listOf(latest("b")), listOf(shelf("AI", 1)),
        )
        assertTrue(out[0] is PodcastHomeSection.Following)
        assertEquals(listOf("x", "y"), (out[0] as PodcastHomeSection.Following).shows.map { it.showId })
        assertTrue(out[1] is PodcastHomeSection.ContinueListening)
        assertTrue(out[2] is PodcastHomeSection.Latest)
        assertTrue(out[3] is PodcastHomeSection.ShowShelf)
        assertEquals(4, out.size)
    }

    @Test fun `orders continue then latest then shows`() {
        val out = buildPodcastHome(emptyList(), listOf(ep("a")), listOf(latest("b")), listOf(shelf("AI", 1)))
        assertTrue(out[0] is PodcastHomeSection.ContinueListening)
        assertTrue(out[1] is PodcastHomeSection.Latest)
        assertTrue(out[2] is PodcastHomeSection.ShowShelf)
        assertEquals(3, out.size)
    }

    @Test fun `drops following, empty shelves and empty episode lists`() {
        val out = buildPodcastHome(emptyList(), emptyList(), emptyList(), listOf(shelf("Empty", 0), shelf("AI", 2)))
        assertEquals(1, out.size)
        assertEquals("AI", (out[0] as PodcastHomeSection.ShowShelf).label)
    }

    @Test fun `continue-only (offline) still renders`() {
        val out = buildPodcastHome(emptyList(), listOf(ep("a")), emptyList(), emptyList())
        assertEquals(1, out.size)
        assertTrue(out[0] is PodcastHomeSection.ContinueListening)
    }
}
