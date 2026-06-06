package com.youtubestream.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SongArtwork
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
                    items(s.data, key = { it.id.name }) { shelf -> ShelfRow(shelf, vm::onPlay) }
                }
            }
        }
    }
}

@Composable
private fun ShelfRow(shelf: Shelf, onPlay: (List<LibrarySong>, Int) -> Unit) {
    Column {
        Text(
            shelf.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(shelf.songs, key = { _, song -> song.id }) { index, song ->
                SongCard(song) { onPlay(shelf.songs, index) }
            }
        }
    }
}

@Composable
private fun SongCard(song: LibrarySong, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.width(140.dp).padding(8.dp)) {
            SongArtwork(song.artworkUrl, size = 124.dp)
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}
