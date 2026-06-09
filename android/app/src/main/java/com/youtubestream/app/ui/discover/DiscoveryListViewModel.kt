package com.youtubestream.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodDetail
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.download.SongDownloads
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads a {title, songs} list via an injected loader; shared by mood-detail and genre-chart screens. */
class DiscoveryListViewModel(
    private val load: suspend () -> MoodDetail,
    private val songDownloads: SongDownloads,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MoodDetail>>(UiState.Loading)
    val state: StateFlow<UiState<MoodDetail>> = _state.asStateFlow()
    val downloadsState: StateFlow<Map<String, ItemDownload>> = songDownloads.downloads

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(load())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Couldn't load")
            }
        }
    }

    fun onSongClick(song: DiscoverySong) =
        songDownloads.enqueue(song.videoId, song.title, song.thumbnailUrl)
}
