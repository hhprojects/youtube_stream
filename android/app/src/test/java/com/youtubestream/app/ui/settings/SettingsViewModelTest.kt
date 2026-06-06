package com.youtubestream.app.ui.settings

import app.cash.turbine.test
import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.LibrarySongDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.settings.SettingsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    private class FakeSettings(initial: String) : SettingsSource {
        val flow = MutableStateFlow(initial)
        override val serverUrl: Flow<String> = flow
        override suspend fun setServerUrl(url: String) { flow.value = url }
    }

    private class FakeDao(initial: List<LibrarySong> = emptyList()) : LibraryDao {
        val songs = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<LibrarySong>> = songs
        override suspend fun exists(id: String) = songs.value.any { it.id == id }
        override suspend fun insert(song: LibrarySong) = songs.update { it + song }
        override suspend fun deleteById(id: String) = songs.update { l -> l.filterNot { it.id == id } }
        override suspend fun clearAllArtwork() = songs.update { l -> l.map { it.copy(artworkUrl = null) } }
    }

    private fun piRepo(onLibrary: suspend () -> LibraryResponseDto) = PiLibraryRepository(object : YoutubeStreamApi {
        override suspend fun search(body: SearchRequestDto) = error("unused")
        override suspend fun download(body: DownloadRequestDto) = error("unused")
        override suspend fun library() = onLibrary()
        override suspend fun deleteFromPi(filename: String) = error("unused")
        override suspend fun updateArtwork(filename: String, body: com.youtubestream.app.data.remote.dto.ArtworkRequestDto) = error("unused")
    })

    @Test fun saveUpdatesTheSource() = runTest {
        val settings = FakeSettings("http://old:3001")
        val vm = SettingsViewModel(settings, piRepo { LibraryResponseDto(emptyList()) }, LibraryRepository(FakeDao()))
        vm.save("http://new:3001")
        runCurrent()
        assertEquals("http://new:3001", settings.flow.value)
    }

    @Test fun saveNormalizesSchemelessUrl() = runTest {
        val settings = FakeSettings("http://old:3001")
        val vm = SettingsViewModel(settings, piRepo { LibraryResponseDto(emptyList()) }, LibraryRepository(FakeDao()))
        vm.save("10.0.0.9:3001")
        runCurrent()
        assertEquals("http://10.0.0.9:3001", settings.flow.value)   // scheme prepended
    }

    @Test fun saveRejectsInvalidUrl() = runTest {
        val settings = FakeSettings("http://old:3001")
        val vm = SettingsViewModel(settings, piRepo { LibraryResponseDto(emptyList()) }, LibraryRepository(FakeDao()))
        vm.save("http://")     // scheme but no host
        runCurrent()
        assertEquals("http://old:3001", settings.flow.value)        // unchanged — not saved
    }

    @Test fun testConnectionOkReportsCount() = runTest {
        val song = LibrarySongDto("a", "T", "A", "Unknown", "a.m4a", "u", 1L, "d")
        val vm = SettingsViewModel(FakeSettings("u"), piRepo { LibraryResponseDto(listOf(song)) }, LibraryRepository(FakeDao()))
        vm.test.test {
            assertEquals(TestResult.Idle, awaitItem())
            vm.testConnection()
            assertEquals(TestResult.Testing, awaitItem())
            assertEquals(TestResult.Ok(1), awaitItem())
        }
    }

    @Test fun testConnectionFailureReportsError() = runTest {
        val vm = SettingsViewModel(FakeSettings("u"), piRepo { throw IOException("down") }, LibraryRepository(FakeDao()))
        vm.test.test {
            awaitItem()                 // Idle
            vm.testConnection()
            awaitItem()                 // Testing
            assertTrue(awaitItem() is TestResult.Failed)
        }
    }

    @Test fun resetArtworkClearsEveryRow() = runTest {
        val dao = FakeDao(listOf(
            LibrarySong("a", "T", "A", 1, "a.m4a", "/p/a.m4a", 1L, 1L, "http://i/a.jpg"),
        ))
        val vm = SettingsViewModel(FakeSettings("u"), piRepo { LibraryResponseDto(emptyList()) }, LibraryRepository(dao))
        vm.resetArtwork()
        runCurrent()
        assertNull(dao.songs.value.single().artworkUrl)
    }
}
