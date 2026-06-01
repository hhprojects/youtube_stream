package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LibraryRepositoryTest {

    private class FakeDao(initial: List<LibrarySong> = emptyList()) : LibraryDao {
        val songs = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<LibrarySong>> = songs
        override suspend fun exists(id: String) = songs.value.any { it.id == id }
        override suspend fun insert(song: LibrarySong) = songs.update { it + song }
        override suspend fun deleteById(id: String) = songs.update { l -> l.filterNot { it.id == id } }
    }

    @Test
    fun deleteRemovesRowAndLocalFile() = runTest {
        val file = File.createTempFile("song", ".m4a").apply { writeText("x") }
        val song = LibrarySong("id1", "T", "A", 1, file.name, file.absolutePath, 1L, 1L)
        val dao = FakeDao(listOf(song))
        val repo = LibraryRepository(dao)

        assertTrue(file.exists())
        repo.delete(song)

        assertFalse(file.exists())          // local file gone
        assertFalse(dao.exists("id1"))      // Room row gone
        assertEquals(emptyList<LibrarySong>(), dao.songs.value)
    }
}
