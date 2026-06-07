package com.youtubestream.app.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.repository.PlaylistRepository
import com.youtubestream.app.playback.PlaybackController
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.library.toPlayableTrack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Header + ordered songs for one playlist's detail page. */
data class PlaylistDetailData(
    val name: String,
    val coverArtUrl: String?,
    val songs: List<LibrarySong>,
)

/**
 * One manual playlist's detail. Combines the playlist row (name/cover) with its joined songs.
 * [closed] flips true when the playlist no longer exists (deleted here or elsewhere) so the
 * screen can pop. Playback goes through the same controller path as the rest of the app.
 */
class PlaylistDetailViewModel(
    private val playlists: PlaylistRepository,
    private val controller: PlaybackController,
    private val playlistId: Long,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    val state: StateFlow<UiState<PlaylistDetailData>> =
        combine(playlists.observePlaylist(playlistId), playlists.observeSongs(playlistId)) { pl, songs ->
            pl?.let { UiState.Content(PlaylistDetailData(it.name, it.coverArtUrl, songs)) } ?: UiState.Loading
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val closed: StateFlow<Boolean> =
        playlists.observePlaylist(playlistId).map { it == null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun play(songs: List<LibrarySong>, startIndex: Int) =
        controller.setQueueAndPlay(songs.map { it.toPlayableTrack() }, startIndex)

    /** Shuffle = play a pre-shuffled queue from the top (player shuffle-mode is untouched). */
    fun shuffle(songs: List<LibrarySong>) =
        controller.setQueueAndPlay(songs.shuffled().map { it.toPlayableTrack() }, 0)

    fun rename(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { playlists.rename(playlistId, trimmed, now()) }
    }

    fun delete() {
        viewModelScope.launch { playlists.delete(playlistId) }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch { playlists.removeSong(playlistId, songId) }
    }

    /** Persist a drag-reordered order. [orderedSongIds] comes from PlaylistReorder.reorder(...). */
    fun reorder(orderedSongIds: List<String>) {
        viewModelScope.launch { playlists.setOrder(playlistId, orderedSongIds) }
    }
}
