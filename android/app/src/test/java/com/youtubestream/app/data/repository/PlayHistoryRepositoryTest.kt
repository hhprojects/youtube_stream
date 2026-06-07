package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.data.local.PlayEventDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayHistoryRepositoryTest {

    private class FakeDao(initial: List<PlayEvent> = emptyList()) : PlayEventDao {
        val events = MutableStateFlow(initial)
        override suspend fun insert(event: PlayEvent) = events.update { it + event }
        override fun observeAll(): Flow<List<PlayEvent>> = events
        // Smart-playlist queries are SQL (JOIN to library_songs) — not exercised here; stubbed empty.
        override fun observeRecentlyPlayed(limit: Int): Flow<List<LibrarySong>> = flowOf(emptyList())
        override fun observeMostPlayed(limit: Int): Flow<List<LibrarySong>> = flowOf(emptyList())
    }

    @Test
    fun recordInsertsEventWithSongIdAndTimestamp() = runTest {
        val dao = FakeDao()
        val repo = PlayHistoryRepository(dao)

        repo.record(songId = "abc", at = 1_000L)

        assertEquals(1, dao.events.value.size)
        assertEquals("abc", dao.events.value.first().songId)
        assertEquals(1_000L, dao.events.value.first().playedAt)
    }

    @Test
    fun observeEmitsDaoStream() = runTest {
        val dao = FakeDao(listOf(PlayEvent(1, "abc", 1L)))
        val repo = PlayHistoryRepository(dao)

        assertEquals(listOf(PlayEvent(1, "abc", 1L)), repo.observe().first())
    }
}
