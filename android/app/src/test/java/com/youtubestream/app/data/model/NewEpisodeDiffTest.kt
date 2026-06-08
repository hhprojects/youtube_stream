package com.youtubestream.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NewEpisodeDiffTest {
    // --- newEpisodesSince: videoId-anchored, newest-first list ---
    @Test fun `anchor at top → nothing new, anchor unchanged`() {
        val d = newEpisodesSince(listOf("c", "b", "a"), "c")
        assertEquals(emptyList<String>(), d.newVideoIds)
        assertEquals("c", d.newAnchor)
    }

    @Test fun `two new above anchor → returns them, advances anchor to top`() {
        val d = newEpisodesSince(listOf("e", "d", "c", "b", "a"), "c")
        assertEquals(listOf("e", "d"), d.newVideoIds)
        assertEquals("e", d.newAnchor)
    }

    @Test fun `null anchor (first check) → nothing new, anchor set to top (no spam)`() {
        val d = newEpisodesSince(listOf("c", "b", "a"), null)
        assertEquals(emptyList<String>(), d.newVideoIds)
        assertEquals("c", d.newAnchor)
    }

    @Test fun `anchor not found (scrolled past limit) → nothing new, anchor reset to top`() {
        val d = newEpisodesSince(listOf("e", "d", "c"), "a")
        assertEquals(emptyList<String>(), d.newVideoIds)
        assertEquals("e", d.newAnchor)
    }

    @Test fun `empty list → nothing new, anchor unchanged`() {
        val d = newEpisodesSince(emptyList(), "c")
        assertEquals(emptyList<String>(), d.newVideoIds)
        assertEquals("c", d.newAnchor)
    }

    // --- newEpisodeNotificationText ---
    @Test fun `no shows → null (post nothing)`() =
        assertEquals(null, newEpisodeNotificationText(emptyList()))

    @Test fun `one show one episode`() =
        assertEquals("New episode from Beyond Coding", newEpisodeNotificationText(listOf(ShowNewEpisodes("Beyond Coding", 1))))

    @Test fun `one show many episodes`() =
        assertEquals("3 new episodes from Beyond Coding", newEpisodeNotificationText(listOf(ShowNewEpisodes("Beyond Coding", 3))))

    @Test fun `multiple shows`() =
        assertEquals("New episodes from 2 shows you follow",
            newEpisodeNotificationText(listOf(ShowNewEpisodes("A", 1), ShowNewEpisodes("B", 2))))
}
