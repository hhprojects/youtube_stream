package com.youtubestream.app.ui.settings

import app.cash.turbine.test
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.LibrarySongDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.settings.SettingsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    private fun piRepo(onLibrary: suspend () -> LibraryResponseDto) = PiLibraryRepository(object : YoutubeStreamApi {
        override suspend fun search(body: SearchRequestDto) = error("unused")
        override suspend fun download(body: DownloadRequestDto) = error("unused")
        override suspend fun library() = onLibrary()
    })

    @Test fun saveUpdatesTheSource() = runTest {
        val settings = FakeSettings("http://old:3001")
        val vm = SettingsViewModel(settings, piRepo { LibraryResponseDto(emptyList()) })
        vm.save("http://new:3001")
        runCurrent()
        assertEquals("http://new:3001", settings.flow.value)
    }

    @Test fun testConnectionOkReportsCount() = runTest {
        val song = LibrarySongDto("a", "T", "A", "Unknown", "a.m4a", "u", 1L, "d")
        val vm = SettingsViewModel(FakeSettings("u"), piRepo { LibraryResponseDto(listOf(song)) })
        vm.test.test {
            assertEquals(TestResult.Idle, awaitItem())
            vm.testConnection()
            assertEquals(TestResult.Testing, awaitItem())
            assertEquals(TestResult.Ok(1), awaitItem())
        }
    }

    @Test fun testConnectionFailureReportsError() = runTest {
        val vm = SettingsViewModel(FakeSettings("u"), piRepo { throw IOException("down") })
        vm.test.test {
            awaitItem()                 // Idle
            vm.testConnection()
            awaitItem()                 // Testing
            assertTrue(awaitItem() is TestResult.Failed)
        }
    }
}
