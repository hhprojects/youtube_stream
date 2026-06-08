package com.youtubestream.app.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistAppendTest {

    @Test fun appendsContiguouslyAfterMaxPosition() {
        val rows = PlaylistAppend.appendedMembers(7L, listOf("a", "b", "c"), maxPosition = 4, now = 100L)
        assertEquals(listOf("a", "b", "c"), rows.map { it.songId })
        assertEquals(listOf(5, 6, 7), rows.map { it.position })
        assertEquals(listOf(7L, 7L, 7L), rows.map { it.playlistId })
        assertEquals(listOf(100L, 100L, 100L), rows.map { it.dateAdded })
    }

    @Test fun startsAtZeroForEmptyPlaylist() {
        val rows = PlaylistAppend.appendedMembers(1L, listOf("x", "y"), maxPosition = -1, now = 1L)
        assertEquals(listOf(0, 1), rows.map { it.position })
    }

    @Test fun emptyInputProducesNoRows() {
        assertEquals(emptyList<String>(), PlaylistAppend.appendedMembers(1L, emptyList(), 3, 1L).map { it.songId })
    }
}
