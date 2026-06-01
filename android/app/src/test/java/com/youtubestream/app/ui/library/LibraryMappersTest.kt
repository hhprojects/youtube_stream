package com.youtubestream.app.ui.library

import com.youtubestream.app.data.local.LibrarySong
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryMappersTest {

    private fun song(localPath: String) =
        LibrarySong("v1", "Title", "Artist", 0, "f.m4a", localPath, 1L, 1L)

    @Test fun mapsFieldsAndFileUri() {
        val track = song("/data/user/0/com.youtubestream.app/files/songs/f.m4a").toPlayableTrack()
        assertEquals("v1", track.mediaId)
        assertEquals("Title", track.title)
        assertEquals("Artist", track.artist)
        assertEquals("file:/data/user/0/com.youtubestream.app/files/songs/f.m4a", track.uri)
    }

    @Test fun encodesSpacesAndHashInFilename() {
        val track = song("/songs/My Song #1.m4a").toPlayableTrack()
        assertEquals("file:/songs/My%20Song%20%231.m4a", track.uri)
    }
}
