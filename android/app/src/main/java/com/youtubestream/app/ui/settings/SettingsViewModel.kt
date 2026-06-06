package com.youtubestream.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.settings.DEFAULT_SERVER_URL
import com.youtubestream.app.data.settings.SettingsSource
import com.youtubestream.app.data.settings.normalizeServerUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TestResult {
    data object Idle : TestResult
    data object Testing : TestResult
    data class Ok(val count: Int) : TestResult
    data class Failed(val message: String) : TestResult
}

class SettingsViewModel(
    private val settings: SettingsSource,
    private val piLibrary: PiLibraryRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    val serverUrl: StateFlow<String> = settings.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_SERVER_URL)

    private val _test = MutableStateFlow<TestResult>(TestResult.Idle)
    val test: StateFlow<TestResult> = _test.asStateFlow()

    fun save(url: String) {
        val normalized = normalizeServerUrl(url)
        if (normalized == null) {
            _test.value = TestResult.Failed("Enter a valid URL like http://192.168.1.5:3001")
            return
        }
        viewModelScope.launch { settings.setServerUrl(normalized) }
    }

    fun reset() {
        viewModelScope.launch { settings.setServerUrl(DEFAULT_SERVER_URL) }
    }

    /** One-time cleanup: blanks artwork on every local row so old wrong guesses show the placeholder. */
    fun resetArtwork() {
        viewModelScope.launch { library.resetAllArtwork() }
    }

    fun testConnection() {
        viewModelScope.launch {
            _test.value = TestResult.Testing
            _test.value = try {
                TestResult.Ok(piLibrary.piLibrary().size)
            } catch (e: Exception) {
                TestResult.Failed(e.message ?: "unreachable")
            }
        }
    }
}
