package com.youtubestream.app.ui.podcast

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeViewTest {
    private fun row(
        id: String,
        downloaded: Boolean = false,
        resume: Long = 0,
        finished: Boolean = false,
        dur: Int = 2700,
    ) = EpisodeRowUi(id, "Ep $id", dur, null, downloaded, if (downloaded) "f_$id" else null, resume, finished)

    @Test fun `play state unplayed when not downloaded`() {
        assertEquals(EpisodePlayState.UNPLAYED, episodePlayState(row("a")))
    }

    @Test fun `play state unplayed when downloaded but not started`() {
        assertEquals(EpisodePlayState.UNPLAYED, episodePlayState(row("a", downloaded = true, resume = 0)))
    }

    @Test fun `play state in progress when downloaded and resumed`() {
        assertEquals(EpisodePlayState.IN_PROGRESS, episodePlayState(row("a", downloaded = true, resume = 5000)))
    }

    @Test fun `play state played when finished`() {
        assertEquals(
            EpisodePlayState.PLAYED,
            episodePlayState(row("a", downloaded = true, resume = 5000, finished = true)),
        )
    }

    @Test fun `view newest keeps remote order`() {
        val rows = listOf(row("a"), row("b"))
        assertEquals(listOf("a", "b"), applyEpisodeView(rows, EpisodeSort.NEWEST, false).map { it.videoId })
    }

    @Test fun `view oldest reverses`() {
        val rows = listOf(row("a"), row("b"))
        assertEquals(listOf("b", "a"), applyEpisodeView(rows, EpisodeSort.OLDEST, false).map { it.videoId })
    }

    @Test fun `unplayed only hides finished`() {
        val rows = listOf(row("a"), row("b", downloaded = true, resume = 9, finished = true))
        assertEquals(listOf("a"), applyEpisodeView(rows, EpisodeSort.NEWEST, true).map { it.videoId })
    }

    @Test fun `unplayed only then oldest applies both`() {
        val rows = listOf(row("a"), row("b", downloaded = true, finished = true), row("c"))
        assertEquals(listOf("c", "a"), applyEpisodeView(rows, EpisodeSort.OLDEST, true).map { it.videoId })
    }

    @Test fun `duration formats`() {
        assertEquals("", formatEpisodeDuration(0))
        assertEquals("45 min", formatEpisodeDuration(2700))
        assertEquals("1 hr 49 min", formatEpisodeDuration(6540))
        assertEquals("1 hr", formatEpisodeDuration(3600))
    }

    @Test fun `subtitle reflects state`() {
        assertEquals("45 min", episodeSubtitle(row("a", dur = 2700)))
        assertEquals(
            "Played · 45 min",
            episodeSubtitle(row("a", downloaded = true, resume = 10, finished = true, dur = 2700)),
        )
    }

    @Test fun `subtitle in progress shows time left`() {
        // 2700s total (45 min); resume 600_000 ms (10 min) → 35 min left
        assertEquals("35 min left", episodeSubtitle(row("a", downloaded = true, resume = 600_000, dur = 2700)))
    }
}
