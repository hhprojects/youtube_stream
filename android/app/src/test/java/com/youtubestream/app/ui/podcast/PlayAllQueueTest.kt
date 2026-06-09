package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.PodcastEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayAllQueueTest {
    private fun ep(videoId: String, finished: Boolean = false, resume: Long = 0L) = PodcastEpisode(
        id = "$videoId.m4a", videoId = videoId, title = videoId, showName = "S", showId = "sh",
        durationSeconds = 100, filename = "$videoId.m4a", localPath = "/p/$videoId.m4a", size = 1,
        dateAdded = 0, resumePositionMs = resume, isFinished = finished,
    )

    @Test fun `null when nothing downloaded`() {
        assertNull(playAllQueue(listOf("v3", "v2", "v1"), emptyList()))
    }

    @Test fun `oldest-first, downloaded-only, skip finished, resume the oldest unfinished`() {
        val remote = listOf("v3", "v2", "v1")                                   // newest-first
        val downloaded = listOf(ep("v3", finished = true), ep("v1", resume = 5_000)) // v2 not downloaded
        val q = playAllQueue(remote, downloaded)!!
        assertEquals(listOf("v1"), q.episodes.map { it.videoId })               // v3 finished skipped, v2 absent
        assertEquals(5_000L, q.startPositionMs)                                 // resume the oldest unfinished
    }

    @Test fun `all finished replays all oldest-first from 0`() {
        val remote = listOf("v2", "v1")
        val downloaded = listOf(ep("v1", finished = true), ep("v2", finished = true))
        val q = playAllQueue(remote, downloaded)!!
        assertEquals(listOf("v1", "v2"), q.episodes.map { it.videoId })         // oldest-first
        assertEquals(0L, q.startPositionMs)
    }

    @Test fun `excludes a downloaded episode not in the show's remote list`() {
        val q = playAllQueue(listOf("v1"), listOf(ep("v1"), ep("vX")))!!
        assertEquals(listOf("v1"), q.episodes.map { it.videoId })
    }
}
