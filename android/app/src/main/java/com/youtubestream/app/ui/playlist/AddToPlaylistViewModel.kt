package com.youtubestream.app.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PlaylistSummary
import com.youtubestream.app.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the "add to playlist" picker. Self-contained so any screen (Library, Search, Now-Playing)
 * can show the sheet with just a song. Adding a song already in the playlist is a no-op (DAO IGNORE).
 */
class AddToPlaylistViewModel(
    private val playlists: PlaylistRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    val summaries: StateFlow<List<PlaylistSummary>> =
        playlists.observeSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(playlistId: Long, songId: String) {
        viewModelScope.launch { playlists.addSong(playlistId, songId, now()) }
    }

    fun createAndAdd(name: String, songId: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = playlists.create(trimmed, now())
            playlists.addSong(id, songId, now())
        }
    }
}
