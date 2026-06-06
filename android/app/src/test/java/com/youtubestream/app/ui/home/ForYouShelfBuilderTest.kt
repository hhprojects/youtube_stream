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
    fun recentlyAdded_sortsByDateAddedDesc_andTakesMax() {
        val songs = (1..15).map { song("s$it", added = it.toLong()) }
        val shelves = ForYouShelfBuilder.build(songs, events = emptyList(), now = now)
        val ra = shelf(shelves, ShelfId.RECENTLY_ADDED)!!
        assertEquals("s15", ra.songs.first().id)             // newest first
        assertEquals(12, ra.songs.size)                      // MAX_ITEMS
    }

    @Test
    fun coldStart_noEvents_showsOnlyRecentlyAdded() {
        val shelves = ForYouShelfBuilder.build(lib("a", "b", "c", "d"), emptyList(), now)
        assertEquals(listOf(ShelfId.RECENTLY_ADDED), shelves.map { it.id })
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
    fun mostPlayed_byCountDesc_tieBreakRecency() {
        val songs = lib("a", "b", "c", "d")
        val events = listOf(
            play("a", 1), play("a", 2), play("a", 3),   // 3
            play("b", 5), play("b", 6),                  // 2, most recent of the 2-counts
            play("c", 4),                                 // 1
            play("d", 7),                                 // 1, more recent than c
        )
        val mp = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.MOST_PLAYED)!!
        assertEquals(listOf("a", "b", "d", "c"), mp.songs.map { it.id })
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
    fun onRepeat_belowMinVisible_isHidden() {
        // Only one qualifying song (< MIN_VISIBLE=4) → no On repeat shelf at all.
        val songs = lib("a")
        val events = List(3) { play("a", now - DAY) }
        assertEquals(null, shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.ON_REPEAT))
    }

    @Test
    fun rediscover_playedBefore_butNotRecently() {
        val songs = lib("old1", "old2", "old3", "old4", "fresh")
        val events = buildList {
            listOf("old1", "old2", "old3", "old4").forEach { add(play(it, now - 60 * DAY)) }  // stale
            add(play("fresh", now - 1 * DAY))                                                  // recent → excluded
        }
        val rd = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.REDISCOVER)!!
        assertTrue(rd.songs.map { it.id }.containsAll(listOf("old1", "old2", "old3", "old4")))
        assertTrue(rd.songs.none { it.id == "fresh" })
    }

    @Test
    fun orphanEvents_forDeletedSongs_areIgnored() {
        val songs = lib("a", "b", "c", "d")            // "ghost" not in library
        val events = listOf(play("ghost", now - 1)) + songs.map { play(it.id, now - 2) }
        val rp = shelf(ForYouShelfBuilder.build(songs, events, now), ShelfId.RECENTLY_PLAYED)!!
        assertTrue(rp.songs.none { it.id == "ghost" })
    }
}
