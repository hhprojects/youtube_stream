package com.youtubestream.app.ui.home

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForYouShelfBuilderTest {

    private val DAY = 24L * 60 * 60 * 1000
    private val now = 1_000_000_000_000L

    private fun song(id: String, added: Long = 0L) =
        LibrarySong(id, "T$id", "A$id", 100, "$id.m4a", "/p/$id.m4a", 1L, added)

    private fun lib(vararg ids: String) = ids.map { song(it) }
    private fun play(id: String, at: Long) = PlayEvent(0, id, at)

    private fun shelf(shelves: List<Shelf>, id: ShelfId) = shelves.firstOrNull { it.id == id }

    @Test
    fun coldStart_noEvents_showsNoShelves() {
        // Both shelves derive from play history, so a no-history library yields nothing.
        val shelves = ForYouShelfBuilder.build(lib("a", "b", "c", "d"), emptyList(), now)
        assertEquals(emptyList<ShelfId>(), shelves.map { it.id })
    }

    @Test
    fun recentlyPlayed_distinctBySong_mostRecentFirst() {
        val songs = lib("a", "b", "c", "d", "e")
        val events = listOf(
            play("a", now - 5), play("b", now - 4), play("a", now - 1),  // a replayed most recently
            play("c", now - 3), play("d", now - 2), play("e", now - 6),
        )
        val rp = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.RECENTLY_PLAYED)!!
        assertEquals(listOf("a", "d", "c", "b", "e"), rp.songs.map { it.id })  // by max(playedAt) desc, distinct
    }

    @Test
    fun onRepeat_countsRecentWindowOnly() {
        // 4 qualifying songs (clears MIN_VISIBLE) so we isolate the WINDOW rule, not the hide rule.
        val songs = lib("a", "b", "c", "d", "old")
        val events = buildList {
            listOf("a", "b", "c", "d").forEach { id -> repeat(3) { add(play(id, now - 1 * DAY)) } }  // 4 qualify
            repeat(3) { add(play("old", now - 40 * DAY)) }                                            // out of window
        }
        val or = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.ON_REPEAT)!!
        assertTrue(or.songs.map { it.id }.containsAll(listOf("a", "b", "c", "d")))
        assertTrue(or.songs.none { it.id == "old" })
    }

    @Test
    fun recentlyPlayed_showsWithJustTwoDistinct() {
        // Quick-resume ("jump back in"): a small history still surfaces Recently played (min 2).
        val songs = lib("a", "b", "c")
        val events = listOf(play("a", now - 2), play("b", now - 1))
        val rp = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.RECENTLY_PLAYED)
        assertEquals(listOf("b", "a"), rp?.songs?.map { it.id })
    }

    @Test
    fun recentlyPlayed_singleDistinct_stillHidden() {
        // One distinct recent song is too sparse for a shelf — stays hidden.
        val songs = lib("a")
        val events = listOf(play("a", now - 1), play("a", now - 2))
        assertEquals(null, shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.RECENTLY_PLAYED))
    }

    @Test
    fun onRepeat_belowMinVisible_isHidden() {
        // Only one qualifying song (< MIN_VISIBLE=4) → no On repeat shelf at all.
        val songs = lib("a")
        val events = List(3) { play("a", now - DAY) }
        assertEquals(null, shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.ON_REPEAT))
    }

    @Test
    fun orphanEvents_forDeletedSongs_areIgnored() {
        val songs = lib("a", "b", "c", "d")            // "ghost" not in library
        val events = listOf(play("ghost", now - 1)) + songs.map { play(it.id, now - 2) }
        val rp = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.RECENTLY_PLAYED)!!
        assertTrue(rp.songs.none { it.id == "ghost" })
    }
}
