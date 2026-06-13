package com.youtubestream.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.repository.LyricsRepository
import com.youtubestream.app.lyrics.LyricsResult
import com.youtubestream.app.lyrics.SongRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads lyrics for the currently-playing song. Scoped above the player sheet so it survives the sheet
 * collapsing/re-expanding (PlayerScreen is conditionally composed) — load() de-dupes by song id, so
 * re-expanding never re-fetches. State is LyricsResult? where null == still loading.
 */
class LyricsViewModel(private val repository: LyricsRepository) : ViewModel() {
    private val _state = MutableStateFlow<LyricsResult?>(null)
    val state: StateFlow<LyricsResult?> = _state.asStateFlow()

    private var loadedId: String? = null

    fun load(ref: SongRef) {
        if (ref.id == loadedId) return   // already loaded/loading this song
        loadedId = ref.id
        _state.value = null              // show Loading while we fetch the new song
        viewModelScope.launch {
            _state.value = repository.getLyrics(ref)
        }
    }
}
