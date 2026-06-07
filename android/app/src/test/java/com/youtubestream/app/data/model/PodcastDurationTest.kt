package com.youtubestream.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastDurationTest {
    @Test fun `hours and minutes`() = assertEquals(6540, parsePodcastDuration("1 hr 49 min"))
    @Test fun `minutes only`() = assertEquals(1380, parsePodcastDuration("23 min"))
    @Test fun `hours only`() = assertEquals(7200, parsePodcastDuration("2 hr"))
    @Test fun `single hour`() = assertEquals(3600, parsePodcastDuration("1 hr"))
    @Test fun `null is zero`() = assertEquals(0, parsePodcastDuration(null))
    @Test fun `unparseable is zero`() = assertEquals(0, parsePodcastDuration("35K views"))
    @Test fun `clock format hh mm ss`() = assertEquals(6540, parsePodcastDuration("1:49:00"))
    @Test fun `clock format mm ss`() = assertEquals(1380, parsePodcastDuration("23:00"))
}
