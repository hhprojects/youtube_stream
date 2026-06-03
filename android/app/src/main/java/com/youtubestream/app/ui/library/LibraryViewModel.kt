package com.youtubestream.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.PiLibraryRepository
import com.youtubestream.app.playback.PlaybackController
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val library: LibraryRepository,
    private val pi: PiLibraryRepository,
    private val controller: PlaybackController,
) : ViewModel() {

    // One-shot user-facing errors (e.g. a failed Pi delete). The screen collects this into a toast.
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors

    /** Room's reactive Flow → Content. Starts as Loading until the first emission arrives. */
    val state: StateFlow<UiState<List<LibrarySong>>> = library.observeLibrary()
        .map<List<LibrarySong>, UiState<List<LibrarySong>>> { UiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Loads the entire library as the queue and starts at [startIndex]. */
    fun play(songs: List<LibrarySong>, startIndex: Int) {
        controller.setQueueAndPlay(songs.map { it.toPlayableTrack() }, startIndex)
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
}
