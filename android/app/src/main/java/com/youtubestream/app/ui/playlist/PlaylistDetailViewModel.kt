package com.youtubestream.app.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.repository.PlayHistoryRepository
import com.youtubestream.app.data.repository.PlaylistRepository
import com.youtubestream.app.playback.PlaybackController
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.library.toPlayableTrack
import com.youtubestream.app.ui.selection.SelectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Header + ordered songs for one playlist's detail page. */
data class PlaylistDetailData(
    val name: String,
    val coverArtUrl: String?,
    val songs: List<LibrarySong>,
)

/**
 * One playlist's detail, for either kind of [PlaylistSource]:
 *  - **Manual**: name/cover from the playlist row, songs from the join; fully editable; [closed]
 *    flips true when the row is deleted (here or elsewhere) so the screen can pop.
 *  - **Smart** (Recently/Most played): songs from the matching [PlayHistoryRepository] query, a
 *    static title, no cover, never closes; edit ops are no-ops ([isEditable] is false and the UI
 *    hides the controls). Playback goes through the same controller path in both modes.
 */
class PlaylistDetailViewModel(
    private val playlists: PlaylistRepository,
    private val playHistory: PlayHistoryRepository,
    private val controller: PlaybackController,
    private val source: PlaylistSource,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    /** Manual playlists can be edited (drag/remove/rename/delete/cover); smart ones are read-only. */
    val isEditable: Boolean = source is PlaylistSource.Manual

    private val _selection = MutableStateFlow(SelectionState())
    /** Multi-select state (manual playlists only); the screen renders it and drives the toggles. */
    val selection: StateFlow<SelectionState> = _selection

    private val songs: Flow<List<LibrarySong>> = when (source) {
        is PlaylistSource.Manual -> playlists.observeSongs(source.id)
        is PlaylistSource.Smart -> when (source.kind) {
            SmartKind.RECENTLY_PLAYED -> playHistory.observeRecentlyPlayed()
            SmartKind.MOST_PLAYED -> playHistory.observeMostPlayed()
        }
    }

    val state: StateFlow<UiState<PlaylistDetailData>> =
        when (source) {
            is PlaylistSource.Manual ->
                combine(playlists.observePlaylist(source.id), songs) { pl, list ->
                    pl?.let { UiState.Content(PlaylistDetailData(it.name, it.coverArtUrl, list)) } ?: UiState.Loading
                }
            is PlaylistSource.Smart ->
                songs.map { list -> UiState.Content(PlaylistDetailData(source.kind.title, null, list)) }
        }
            .onEach { ui ->
                if (ui is UiState.Content) {
                    _selection.update { it.prune(ui.data.songs.mapTo(HashSet()) { s -> s.id }) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Manual playlists vanish when deleted (→ pop the screen); smart playlists never close. */
    val closed: StateFlow<Boolean> =
        when (source) {
            is PlaylistSource.Manual -> playlists.observePlaylist(source.id).map { it == null }
            is PlaylistSource.Smart -> flowOf(false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun play(list: List<LibrarySong>, startIndex: Int) =
        controller.setQueueAndPlay(list.map { it.toPlayableTrack() }, startIndex)

    /** Shuffle = play a pre-shuffled queue from the top (player shuffle-mode is untouched). */
    fun shuffle(list: List<LibrarySong>) =
        controller.setQueueAndPlay(list.shuffled().map { it.toPlayableTrack() }, 0)

    // ---- edit ops: only meaningful for a Manual source; no-ops otherwise (the UI also hides them) ----
    private val manualId: Long? get() = (source as? PlaylistSource.Manual)?.id

    fun rename(name: String) {
        val id = manualId ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { playlists.rename(id, trimmed, now()) }
    }

    fun delete() {
        val id = manualId ?: return
        viewModelScope.launch { playlists.delete(id) }
    }

    fun removeSong(songId: String) {
        val id = manualId ?: return
        viewModelScope.launch { playlists.removeSong(id, songId) }
    }

    fun enterSelection(initialId: String? = null) = _selection.update { it.enter(initialId) }
    fun exitSelection() { _selection.value = SelectionState() }
    fun toggle(id: String) = _selection.update { it.toggle(id) }
    fun toggleSelectAll(allIds: List<String>) = _selection.update { it.toggleSelectAll(allIds) }

    /** Remove the current selection from this (manual) playlist, then exit selection mode. */
    fun removeSelected() {
        val id = manualId ?: return
        val ids = _selection.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            playlists.removeSongs(id, ids)
            _selection.value = SelectionState()
        }
    }

    /** Persist a drag-reordered order. [orderedSongIds] comes from PlaylistReorder.reorder(...). */
    fun reorder(orderedSongIds: List<String>) {
        val id = manualId ?: return
        viewModelScope.launch { playlists.setOrder(id, orderedSongIds) }
    }

    /** Set a custom cover (a pasted image URL), or null to fall back to the auto first-song art. */
    fun setCover(url: String?) {
        val id = manualId ?: return
        viewModelScope.launch { playlists.setCover(id, url, now()) }
    }
}
