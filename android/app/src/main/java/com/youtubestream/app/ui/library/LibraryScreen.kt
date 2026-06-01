package com.youtubestream.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { LibraryViewModel(it.libraryRepository, it.playbackConnection) }
    val state by vm.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<LibrarySong?>(null) }

    Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${s.message}")
            is UiState.Content -> if (s.data.isEmpty()) {
                Text("No downloads yet. Search for a song and tap Download.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(s.data, key = { _, song -> song.id }) { index, song ->
                        LibraryRow(
                            song = song,
                            onPlay = { vm.play(s.data, index) },
                            onDelete = { pendingDelete = song },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = { Text("Removes \"${song.title}\" from this device. The Pi copy stays for re-import.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(song); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LibraryRow(song: LibrarySong, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1)
            Text(song.artist, maxLines = 1)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}
