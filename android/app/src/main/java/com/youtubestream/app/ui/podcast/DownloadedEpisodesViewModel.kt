package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.repository.PodcastSource
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

/**
 * Backs the global "Downloaded episodes" screen. Observes every downloaded episode (all shows) via
 * the existing reactive Flow, holds the reused multi-select [SelectionState], and deletes single or
 * bulk — locally or everywhere. Pure selection logic lives in SelectionState (already tested); this
 * is Flow/coroutine glue, so it's verified by build + on-device, not new unit tests.
 */
class DownloadedEpisodesViewModel(
    private val repo: PodcastSource,
    private val play: (PodcastEpisode) -> Unit,
) : ViewModel() {

    // One-shot user-facing errors (e.g. a failed Pi delete). The screen collects this into a toast.
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors

    private val _selection = MutableStateFlow(SelectionState())
    /** Multi-select state for the list; the screen renders it and drives the toggles. */
    val selection: StateFlow<SelectionState> = _selection

    /** Every downloaded episode (all shows), newest-first — Room's reactive Flow. Loading until first emit. */
    val state: StateFlow<UiState<List<PodcastEpisode>>> = repo.observeDownloadedEpisodes()
        .onEach { eps -> _selection.update { it.prune(eps.mapTo(HashSet()) { e -> e.id }) } }
        .map<List<PodcastEpisode>, UiState<List<PodcastEpisode>>> { UiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun enterSelection(initialId: String? = null) = _selection.update { it.enter(initialId) }
    fun exitSelection() { _selection.value = SelectionState() }
    fun toggle(id: String) = _selection.update { it.toggle(id) }
    fun toggleSelectAll(allIds: List<String>) = _selection.update { it.toggleSelectAll(allIds) }

    /** Play an already-downloaded episode by local id (= filename); the play lambda applies resume. */
    fun onPlayDownloaded(localId: String) {
        viewModelScope.launch { repo.getEpisode(localId)?.let(play) }
    }

    /** Single local-only delete (file + Room row). */
    fun onDeleteDownload(localId: String) {
        viewModelScope.launch { repo.getEpisode(localId)?.let { repo.deleteEpisode(it) } }
    }

    /** Single delete everywhere: Pi first; on failure the local copy is kept + an error shown. */
    fun onDeleteEverywhere(localId: String) {
        viewModelScope.launch {
            val ep = repo.getEpisode(localId) ?: return@launch
            try {
                repo.deleteEpisodeEverywhere(ep)
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Couldn't delete from the Pi")
            }
        }
    }

    /** Bulk local-only delete of the current selection, then exit selection mode. */
    fun deleteSelectedDownloads(episodes: List<PodcastEpisode>) {
        val targets = episodes.filter { it.id in _selection.value.selectedIds }
        viewModelScope.launch {
            targets.forEach { repo.deleteEpisode(it) }
            _selection.value = SelectionState()
        }
    }

    /**
     * Bulk delete everywhere: fan the per-episode Pi-first deletes out concurrently (mirrors the
     * single path — a failed Pi call leaves that local copy intact). Report a summary if any failed,
     * then exit selection mode.
     */
    fun deleteSelectedEverywhere(episodes: List<PodcastEpisode>) {
        val targets = episodes.filter { it.id in _selection.value.selectedIds }
        viewModelScope.launch {
            val outcomes = targets.map { ep ->
                async { try { repo.deleteEpisodeEverywhere(ep); true } catch (e: Exception) { false } }
            }.awaitAll()
            val ok = outcomes.count { it }
            if (ok < targets.size) {
                _errors.tryEmit("$ok of ${targets.size} removed from the server")
            }
            _selection.value = SelectionState()
        }
    }
}
