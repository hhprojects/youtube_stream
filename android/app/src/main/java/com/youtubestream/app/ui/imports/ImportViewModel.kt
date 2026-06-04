package com.youtubestream.app.ui.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.network.ReachabilitySource
import com.youtubestream.app.data.network.ServerStatus
import com.youtubestream.app.data.repository.ImportDownloadManager
import com.youtubestream.app.data.repository.ImportItemState
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportViewModel(
    private val pi: PiLibraryRepository,
    private val library: LibraryRepository,
    private val importManager: ImportDownloadManager,
    private val reachability: ReachabilitySource,
) : ViewModel() {

    private val _pi = MutableStateFlow<UiState<List<PiSong>>>(UiState.Loading)

    // Match on filename, not id: a downloaded song's row has id=videoId while the same file on the Pi
    // has id=filename, so an id-based diff would list already-downloaded songs forever (and re-importing
    // would insert a duplicate Room row). filename is the stable identity across the download/import paths.
    private val localFilenames = library.observeLibrary()
        .map { songs -> songs.map { it.filename }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Importable = Pi songs whose file isn't already on the device (reactive: imports drop off). */
    val state: StateFlow<UiState<List<PiSong>>> =
        combine(_pi, localFilenames) { piState, local ->
            if (piState is UiState.Content) UiState.Content(piState.data.filter { it.filename !in local })
            else piState
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    /** Bulk-import progress, owned by the app-scoped manager so it survives leaving this screen. */
    val downloads: StateFlow<Map<String, ImportItemState>> = importManager.downloads

    // One-shot user-facing errors (e.g. a failed Pi delete). The screen collects this into a toast.
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors

    /**
     * App-wide Pi reachability — the screen shows a banner and gates Pi actions on this. No dedicated probe
     * on entry: [refresh] below already does GET /api/library (now fast-connect), which the interceptor turns
     * into a reachability report — so a separate probe would just be a duplicate call.
     */
    val status: StateFlow<ServerStatus> = reachability.status

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

    /**
     * Deletes the song from the Pi (these are Pi-only songs, so there's no local copy to touch).
     * On success it drops out of [_pi] so the row disappears reactively; on failure the row stays
     * and the user sees an error.
     */
    fun deleteFromPi(song: PiSong) {
        viewModelScope.launch {
            try {
                pi.delete(song.filename)
                _pi.update { st ->
                    if (st is UiState.Content) UiState.Content(st.data.filterNot { it.id == song.id }) else st
                }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Couldn't delete from the Pi")
            }
        }
    }

    /**
     * Hands the selected songs to the app-scoped [importManager]. Downloads continue (and progress keeps
     * updating) even if the user navigates away from Import — unlike a `viewModelScope` loop, which the
     * back-stack would cancel.
     */
    fun downloadSelected(importable: List<PiSong>) {
        val toGet = importable.filter { it.id in _selected.value }
        if (toGet.isEmpty()) return
        _selected.value = emptySet()                     // selection consumed
        importManager.enqueue(toGet)
    }
}
