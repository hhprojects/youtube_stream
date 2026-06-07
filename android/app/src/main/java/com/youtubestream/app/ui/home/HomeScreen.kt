package com.youtubestream.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.library.toPlayableTrack

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { c ->
        HomeViewModel(
            observeLibrary = { c.libraryRepository.observeLibrary() },
            observeHistory = { c.playHistoryRepository.observe() },
            play = { songs, index ->
                c.playbackConnection.setQueueAndPlay(songs.map { it.toPlayableTrack() }, index)
            },
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${s.message}")
            is UiState.Content -> if (s.data.isEmpty()) {
                Text("No music yet. Download a song and it'll show up here.")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(s.data, key = { it.id.name }) { shelf ->
                        ShelfCardRow(
                            title = shelf.title,
                            cards = shelf.songs.map { it.toCard() },
                            downloadingKeys = emptySet(),
                        ) { index -> vm.onPlay(shelf.songs, index) }
                    }
                }
            }
        }
    }
}

private fun com.youtubestream.app.data.local.LibrarySong.toCard() =
    ShelfCardUi(key = id, title = title, artist = artist, artworkUrl = artworkUrl)
