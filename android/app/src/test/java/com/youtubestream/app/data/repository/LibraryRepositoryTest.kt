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
        override suspend fun clearAllArtwork() = songs.update { list -> list.map { it.copy(artworkUrl = null) } }
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

    @Test
    fun deleteByIdRemovesRowOnly() = runTest {
        val song = LibrarySong("id1", "T", "A", 1, "f.m4a", "/gone/f.m4a", 1L, 1L)
        val dao = FakeDao(listOf(song))
        val repo = LibraryRepository(dao)

        repo.deleteById("id1")

        assertFalse(dao.exists("id1"))
    }

    @Test
    fun setArtworkInsertsRowWithNewUrl() = runTest {
        val song = LibrarySong("id1", "T", "A", 1, "f.m4a", "/p/f.m4a", 1L, 1L)
        val dao = FakeDao()
        val repo = LibraryRepository(dao)

        repo.setArtwork(song, "http://i/new.jpg")

        assertEquals("http://i/new.jpg", dao.songs.value.last().artworkUrl)
    }

    @Test
    fun resetAllArtworkClearsEveryUrl() = runTest {
        val dao = FakeDao(listOf(
            LibrarySong("a", "T", "A", 1, "a.m4a", "/p/a.m4a", 1L, 1L, "http://i/a.jpg"),
            LibrarySong("b", "T", "A", 1, "b.m4a", "/p/b.m4a", 1L, 1L, "http://i/b.jpg"),
        ))
        val repo = LibraryRepository(dao)

        repo.resetAllArtwork()

        assertEquals(listOf(null, null), dao.songs.value.map { it.artworkUrl })
    }
}
