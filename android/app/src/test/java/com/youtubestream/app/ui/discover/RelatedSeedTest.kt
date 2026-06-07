package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.local.PlayEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelatedSeedTest {

    @Test fun picksMostRecentVideoIdPlay() {
        val history = listOf(
            PlayEvent(songId = "dQw4w9WgXcQ", playedAt = 100),
            PlayEvent(songId = "abcdefghijk", playedAt = 300),  // newest, valid 11-char id
            PlayEvent(songId = "kJQP7kiw5Fk", playedAt = 200),
        )
        assertEquals("abcdefghijk", selectRelatedSeed(history))
    }

    @Test fun skipsImportedFilenameIds() {
        val history = listOf(
            PlayEvent(songId = "Some_Imported_Song.m4a", playedAt = 500),  // newest but not a videoId
            PlayEvent(songId = "dQw4w9WgXcQ", playedAt = 400),
        )
        assertEquals("dQw4w9WgXcQ", selectRelatedSeed(history))
    }

    @Test fun nullWhenNoEligibleSeed() {
        assertNull(selectRelatedSeed(emptyList()))
        assertNull(selectRelatedSeed(listOf(PlayEvent(songId = "x.m4a", playedAt = 1))))
    }
}
