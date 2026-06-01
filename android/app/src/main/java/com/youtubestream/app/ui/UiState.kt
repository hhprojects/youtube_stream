package com.youtubestream.app.ui

/** One immutable screen state. Empty content is `Content(emptyList())` — screens render that as "no results". */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
