package com.youtubestream.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Idle)
    val state: StateFlow<UiState<List<SearchResult>>> = _state.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repo.search(query.trim()))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Search failed")
            }
        }
    }
}
