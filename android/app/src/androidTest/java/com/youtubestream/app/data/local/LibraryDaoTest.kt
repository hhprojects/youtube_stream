package com.youtubestream.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: LibraryDao

    private val song = LibrarySong("id1", "T", "A", 100, "f.m4a", "/p/f.m4a", 123L, 1L)

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).build()
        dao = db.libraryDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertThenObserveEmitsTheSong() = runTest {
        dao.observeAll().test {
            assertEquals(emptyList<LibrarySong>(), awaitItem())
            dao.insert(song)
            assertEquals(listOf(song), awaitItem())
        }
    }

    @Test
    fun deleteByIdRemovesIt() = runTest {
        dao.insert(song)
        assertEquals(true, dao.exists("id1"))
        dao.deleteById("id1")
        assertEquals(false, dao.exists("id1"))
    }
}
