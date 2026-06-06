package com.youtubestream.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val observeLibrary: () -> Flow<List<LibrarySong>>,
    private val observeHistory: () -> Flow<List<PlayEvent>>,
    private val play: (songs: List<LibrarySong>, startIndex: Int) -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    val state: StateFlow<UiState<List<Shelf>>> =
        combine(observeLibrary(), observeHistory()) { songs, events ->
            UiState.Content(ForYouShelfBuilder.build(songs, events, clock()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onPlay(songs: List<LibrarySong>, startIndex: Int) = play(songs, startIndex)
}
