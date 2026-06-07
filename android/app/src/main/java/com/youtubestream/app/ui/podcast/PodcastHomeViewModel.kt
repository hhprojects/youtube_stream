package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastHomeViewModel(private val repo: PodcastSource) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<PodcastShelf>>>(UiState.Loading)
    val state: StateFlow<UiState<List<PodcastShelf>>> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repo.home())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Couldn't load podcasts")
            }
        }
    }
}
