package com.youtubestream.app.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistReorderTest {

    @Test
    fun reorder_movesItemDown() {
        assertEquals(
            listOf("b", "c", "a", "d"),
            PlaylistReorder.reorder(listOf("a", "b", "c", "d"), from = 0, to = 2),
        )
    }

    @Test
    fun reorder_movesItemUp() {
        assertEquals(
            listOf("a", "d", "b", "c"),
            PlaylistReorder.reorder(listOf("a", "b", "c", "d"), from = 3, to = 1),
        )
    }

    @Test
    fun reorder_isNoOpWhenIndicesEqual() {
        assertEquals(
            listOf("a", "b", "c"),
            PlaylistReorder.reorder(listOf("a", "b", "c"), from = 1, to = 1),
        )
    }

    @Test
    fun reorder_movesToFirst() {
        assertEquals(
            listOf("c", "a", "b"),
            PlaylistReorder.reorder(listOf("a", "b", "c"), from = 2, to = 0),
        )
    }
}
