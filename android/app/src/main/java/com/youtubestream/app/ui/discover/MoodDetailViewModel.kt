package com.youtubestream.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodDetail
import com.youtubestream.app.data.repository.DiscoverySource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoodDetailViewModel(
    private val key: String,
    private val source: DiscoverySource,
    private val downloads: DiscoveryDownloads,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MoodDetail>>(UiState.Loading)
    val state: StateFlow<UiState<MoodDetail>> = _state.asStateFlow()
    val downloadsState: StateFlow<Map<String, ItemDownload>> = downloads.downloads

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(source.moodSongs(key))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Couldn't load this mood")
            }
        }
    }

    fun onSongClick(song: DiscoverySong) =
        downloads.download(viewModelScope, song.videoId, song.title, song.thumbnailUrl)
}
