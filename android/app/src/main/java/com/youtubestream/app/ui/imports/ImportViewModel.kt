package com.youtubestream.app.ui.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.repository.Importer
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-song bulk-import status. `Completed` isn't here — a finished id leaves the importable list. */
sealed interface ImportItemState {
    data class Downloading(val fraction: Float) : ImportItemState
    data class Failed(val message: String) : ImportItemState
}

class ImportViewModel(
    private val pi: PiLibraryRepository,
    private val library: LibraryRepository,
    private val importer: Importer,
) : ViewModel() {

    private val _pi = MutableStateFlow<UiState<List<PiSong>>>(UiState.Loading)

    private val localIds = library.observeLibrary()
        .map { songs -> songs.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Importable = Pi songs whose id isn't already in the local library (reactive: imports drop off). */
    val state: StateFlow<UiState<List<PiSong>>> =
        combine(_pi, localIds) { piState, local ->
            if (piState is UiState.Content) UiState.Content(piState.data.filter { it.id !in local })
            else piState
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, ImportItemState>>(emptyMap())
    val downloads: StateFlow<Map<String, ImportItemState>> = _downloads.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _pi.value = UiState.Loading
            _pi.value = try {
                UiState.Content(pi.piLibrary())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Couldn't reach the Pi")
            }
        }
    }

    fun toggle(id: String) {
        _selected.update { if (id in it) it - id else it + id }
    }

    /** Imports the currently-selected songs from [importable], sequentially, with per-item progress. */
    fun downloadSelected(importable: List<PiSong>) {
        // Exclude ids already importing — re-tapping Download mid-import must not start a second
        // stream writing the same target file (mirrors SearchViewModel.download's in-flight guard).
        val toGet = importable.filter {
            it.id in _selected.value && _downloads.value[it.id] !is ImportItemState.Downloading
        }
        if (toGet.isEmpty()) return
        _selected.value = emptySet()                     // selection consumed
        viewModelScope.launch {
            for (song in toGet) {
                importer.importSong(song).collect { st ->
                    _downloads.update { current ->
                        when (st) {
                            is DownloadState.InProgress ->
                                current + (song.id to ImportItemState.Downloading(st.fraction))
                            is DownloadState.Completed ->
                                current - song.id        // inserted → drops off importable via the Room flow
                            is DownloadState.Failed ->
                                current + (song.id to ImportItemState.Failed(st.error.message ?: "Download failed"))
                        }
                    }
                }
            }
        }
    }
}
