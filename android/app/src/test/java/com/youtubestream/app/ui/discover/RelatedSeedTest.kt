package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelatedSeedTest {
    private fun song(id: String, videoId: String?) =
        LibrarySong(
            id = id, title = id, artist = "", durationSeconds = 0, filename = id,
            localPath = "", size = 0, dateAdded = 0, artworkUrl = null, videoId = videoId,
        )

    @Test fun picksMostRecentPlayWithAVideoId() {
        val songs = listOf(song("a.m4a", "dQw4w9WgXcQ"), song("b.m4a", "kJQP7kiw5Fk"))
        val history = listOf(
            PlayEvent(songId = "a.m4a", playedAt = 100),
            PlayEvent(songId = "b.m4a", playedAt = 300),  // newest, has a videoId
        )
        assertEquals("kJQP7kiw5Fk", selectRelatedSeed(history, songs))
    }

    @Test fun skipsPlaysWhoseSongHasNoVideoId() {
        val songs = listOf(song("imported.m4a", null), song("a.m4a", "dQw4w9WgXcQ"))
        val history = listOf(
            PlayEvent(songId = "imported.m4a", playedAt = 500),  // newest but no videoId
            PlayEvent(songId = "a.m4a", playedAt = 400),
        )
        assertEquals("dQw4w9WgXcQ", selectRelatedSeed(history, songs))
    }

    @Test fun nullWhenNoEligibleSeed() {
        assertNull(selectRelatedSeed(emptyList(), emptyList()))
        assertNull(
            selectRelatedSeed(
                listOf(PlayEvent(songId = "x.m4a", playedAt = 1)),
                listOf(song("x.m4a", null)),
            ),
        )
    }
}
