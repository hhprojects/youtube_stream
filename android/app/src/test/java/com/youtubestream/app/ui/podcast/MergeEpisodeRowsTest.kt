package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeEpisodeRowsTest {
    private fun remote(id: String) = PodcastEpisodeItem(id, "Ep $id", 600, null, null, null)
    private fun local(videoId: String, pos: Long, done: Boolean) = PodcastEpisode(
        id = "file_$videoId", videoId = videoId, title = "Ep", showName = "S", showId = "sh",
        durationSeconds = 600, filename = "file_$videoId", localPath = "/p", size = 1, dateAdded = 0,
        resumePositionMs = pos, isFinished = done,
    )

    @Test fun `not downloaded`() {
        val rows = mergeEpisodeRows(listOf(remote("a")), emptyList())
        assertEquals(false, rows[0].downloaded)
        assertEquals(null, rows[0].localId)
        assertEquals(0L, rows[0].resumePositionMs)
    }

    @Test fun `downloaded with resume`() {
        val rows = mergeEpisodeRows(listOf(remote("a")), listOf(local("a", 1234L, false)))
        assertEquals(true, rows[0].downloaded)
        assertEquals("file_a", rows[0].localId)
        assertEquals(1234L, rows[0].resumePositionMs)
        assertEquals(false, rows[0].finished)
    }

    @Test fun `downloaded and finished`() {
        val rows = mergeEpisodeRows(listOf(remote("a")), listOf(local("a", 600_000L, true)))
        assertEquals(true, rows[0].finished)
    }

    @Test fun `order follows remote (newest-first)`() {
        val rows = mergeEpisodeRows(listOf(remote("a"), remote("b")), listOf(local("b", 5L, false)))
        assertEquals(listOf("a", "b"), rows.map { it.videoId })
    }
}
