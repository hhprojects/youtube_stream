package com.youtubestream.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.data.util.YouTubeUrl
import com.youtubestream.app.playback.PlaybackController
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.selection.SelectionState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val library: LibraryRepository,
    private val pi: PiLibraryRepository,
    private val controller: PlaybackController,
) : ViewModel() {

    // One-shot user-facing errors (e.g. a failed Pi delete). The screen collects this into a toast.
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors

    private val _selection = MutableStateFlow(SelectionState())
    /** Multi-select state for the All Songs list; the screen renders it and drives the toggles. */
    val selection: StateFlow<SelectionState> = _selection

    /** Room's reactive Flow → Content. Starts as Loading until the first emission arrives. */
    val state: StateFlow<UiState<List<LibrarySong>>> = library.observeLibrary()
        .onEach { songs -> _selection.update { it.prune(songs.mapTo(HashSet()) { s -> s.id }) } }
        .map<List<LibrarySong>, UiState<List<LibrarySong>>> { UiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Loads the entire library as the queue and starts at [startIndex]. */
    fun play(songs: List<LibrarySong>, startIndex: Int) {
        controller.setQueueAndPlay(songs.map { it.toPlayableTrack() }, startIndex)
    }

    /** Shuffle = play a pre-shuffled queue from the top (player shuffle-mode is untouched). */
    fun playShuffled(songs: List<LibrarySong>) {
        controller.setQueueAndPlay(songs.shuffled().map { it.toPlayableTrack() }, 0)
    }

    fun enterSelection(initialId: String? = null) = _selection.update { it.enter(initialId) }
    fun exitSelection() { _selection.value = SelectionState() }
    fun toggle(id: String) = _selection.update { it.toggle(id) }
    fun toggleSelectAll(allIds: List<String>) = _selection.update { it.toggleSelectAll(allIds) }

    /** Bulk local-only delete of the current selection, then exit selection mode. */
    fun deleteSelectedDownloads(songs: List<LibrarySong>) {
        val targets = songs.filter { it.id in _selection.value.selectedIds }
        viewModelScope.launch {
            library.deleteAll(targets)
            _selection.value = SelectionState()
        }
    }

    /**
     * Bulk delete everywhere: fan the Pi deletes out concurrently (mirrors single-song deleteEverywhere
     * — Pi first, local only for the ones the Pi accepted). Reports a summary if any Pi delete failed,
     * then exits selection mode.
     */
    fun deleteSelectedEverywhere(songs: List<LibrarySong>) {
        val targets = songs.filter { it.id in _selection.value.selectedIds }
        viewModelScope.launch {
            val outcomes = targets.map { s ->
                async {
                    try { pi.delete(s.filename); s to true } catch (e: Exception) { s to false }
                }
            }.awaitAll()
            val piDeleted = outcomes.filter { it.second }.map { it.first }
            library.deleteAll(piDeleted)
            if (outcomes.any { !it.second }) {
                _errors.tryEmit("${piDeleted.size} of ${targets.size} removed from the server")
            }
            _selection.value = SelectionState()
        }
    }

    /** Local-only delete (Room row + file); the Pi copy stays for re-import. The list refreshes via the Flow. */
    fun delete(song: LibrarySong) {
        viewModelScope.launch { library.delete(song) }
    }

    /**
     * Deletes the local copy AND the Pi copy. Pi first (the fallible network op): if it throws,
     * the local copy is left intact and the user sees an error and can retry / fall back to local-only.
     */
    fun deleteEverywhere(song: LibrarySong) {
        viewModelScope.launch {
            try {
                pi.delete(song.filename)
                library.delete(song)
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Couldn't delete from the Pi")
            }
        }
    }

    /**
     * Swaps a song's artwork to the thumbnail of a pasted YouTube URL. Pi-first (like deleteEverywhere):
     * persist on the Pi so it survives re-import + reaches other devices, then mirror into Room. A bad URL
     * or a Pi failure surfaces as a one-shot error; the local row is left untouched.
     */
    fun editArtwork(song: LibrarySong, url: String) {
        val videoId = YouTubeUrl.extractVideoId(url)
        if (videoId == null) {
            _errors.tryEmit("That doesn't look like a YouTube link.")
            return
        }
        viewModelScope.launch {
            try {
                val thumbnail = pi.updateArtwork(song.filename, videoId)
                library.setArtwork(song, thumbnail)
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Couldn't update artwork on the Pi")
            }
        }
    }
}
