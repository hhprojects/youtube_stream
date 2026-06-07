package com.youtubestream.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PlaylistSummary
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PlaylistRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the playlists-first Library landing renders: the user's playlists + the All-Songs count. */
data class LibraryHomeData(
    val playlists: List<PlaylistSummary>,
    val allSongsCount: Int,
)

/**
 * Landing of the Library tab. Combines the playlist summaries with the library size (for the
 * pinned "All songs" row) into one [UiState]. Mirrors the existing Flow→stateIn pattern;
 * the clock is injected so it stays off the hot path and testable, like PlayHistoryRepository.
 */
class LibraryHomeViewModel(
    private val playlists: PlaylistRepository,
    library: LibraryRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    val state: StateFlow<UiState<LibraryHomeData>> =
        combine(playlists.observeSummaries(), library.observeLibrary()) { pls, songs ->
            UiState.Content(LibraryHomeData(pls, songs.size))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { playlists.create(trimmed, now()) }
    }
}
