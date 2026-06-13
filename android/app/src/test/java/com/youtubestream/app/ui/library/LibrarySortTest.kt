package com.youtubestream.app.ui.library

import com.youtubestream.app.data.local.LibrarySong
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortTest {
    private fun song(id: String, title: String, artist: String, dateAdded: Long) =
        LibrarySong(id, title, artist, 0, "$id.m4a", "/p/$id", 1L, dateAdded)

    private val a = song("1", "Banana", "Zed", 100)
    private val b = song("2", "apple", "Alpha", 300)
    private val c = song("3", "Cherry", "mike", 200)
    private val all = listOf(a, b, c)

    @Test fun `title sort is case-insensitive A to Z`() {
        // apple, Banana, Cherry
        assertEquals(listOf("2", "1", "3"), sortLibrary(all, LibrarySort.TITLE).map { it.id })
    }

    @Test fun `artist sort is case-insensitive A to Z`() {
        // Alpha, mike, Zed
        assertEquals(listOf("2", "3", "1"), sortLibrary(all, LibrarySort.ARTIST).map { it.id })
    }

    @Test fun `recently added is newest dateAdded first`() {
        // 300, 200, 100
        assertEquals(listOf("2", "3", "1"), sortLibrary(all, LibrarySort.RECENTLY_ADDED).map { it.id })
    }

    @Test fun `oldest is smallest dateAdded first`() {
        assertEquals(listOf("1", "3", "2"), sortLibrary(all, LibrarySort.OLDEST).map { it.id })
    }

    @Test fun `filter matches title or artist case-insensitively`() {
        assertEquals(listOf("1"), filterLibrary(all, "zed").map { it.id }) // artist "Zed"
        assertEquals(listOf("3"), filterLibrary(all, "CHER").map { it.id }) // title "Cherry"
    }

    @Test fun `blank filter returns all unchanged`() {
        assertEquals(all, filterLibrary(all, "   "))
    }
}
