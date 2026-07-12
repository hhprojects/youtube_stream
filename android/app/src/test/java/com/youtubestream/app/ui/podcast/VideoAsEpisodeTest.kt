package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the video→episode contract: channel-name grouping under a synthetic, never-navigable show id. */
class VideoAsEpisodeTest {

    private val video = SearchResult(
        id = "abc123",
        title = "Great Talk",
        channel = "Conf Channel",
        durationSeconds = 1830,
        thumbnailUrl = "https://i.ytimg.com/vi/abc123/hqdefault.jpg",
    )

    @Test fun `maps a video to an episode grouped under its channel`() {
        val (show, episode) = videoAsEpisode(video)
        assertEquals("video:abc123", show.showId)
        assertEquals("Conf Channel", show.title)
        assertTrue(show.episodes.isEmpty())
        assertEquals("abc123", episode.videoId)
        assertEquals("Great Talk", episode.title)
        assertEquals(1830, episode.durationSeconds)
        assertEquals(video.thumbnailUrl, episode.artworkUrl)
        assertEquals(video.thumbnailUrl, show.artworkUrl)
        assertNull(episode.publishedDate)
    }

    @Test fun `a missing duration maps to zero`() {
        val (_, episode) = videoAsEpisode(video.copy(durationSeconds = null))
        assertEquals(0, episode.durationSeconds)
    }
}
