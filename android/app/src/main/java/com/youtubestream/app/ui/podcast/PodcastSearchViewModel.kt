package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Podcast shows-search: Idle until a non-blank query is submitted, then Loading → Content/Error. */
class PodcastSearchViewModel(private val repo: PodcastSource) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<PodcastShowCard>>>(UiState.Idle)
    val state: StateFlow<UiState<List<PodcastShowCard>>> = _state.asStateFlow()

    fun search(rawQuery: String) {
        val q = rawQuery.trim()
        if (q.isBlank()) {
            _state.value = UiState.Idle
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = try {
                UiState.Content(repo.search(q))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Search failed")
            }
        }
    }
}
