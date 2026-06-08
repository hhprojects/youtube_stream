package com.youtubestream.app.ui.library

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.util.YouTubeUrl
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SongArtwork
import com.youtubestream.app.ui.components.SongRow
import com.youtubestream.app.ui.playlist.AddToPlaylistSheet

@Composable
fun LibraryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel { LibraryViewModel(it.libraryRepository, it.piLibraryRepository, it.playbackConnection) }
    val state by vm.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<LibrarySong?>(null) }
    var editing by remember { mutableStateOf<LibrarySong?>(null) }
    var addingTo by remember { mutableStateOf<LibrarySong?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.errors.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            Text("All songs", style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val s = state) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${s.message}")
            is UiState.Content -> if (s.data.isEmpty()) {
                Text("No downloads yet. Search for a song and tap Download.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(s.data, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.artworkUrl,
                            onClick = { vm.play(s.data, index) },
                            trailing = {
                                IconButton(onClick = { addingTo = song }) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                                }
                                IconButton(onClick = { editing = song }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit artwork")
                                }
                                IconButton(onClick = { pendingDelete = song }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            },
                        )
                    }
                }
            }
        }
        }
    }

    pendingDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${song.title}\"?") },
            text = {
                Text(
                    "\"Delete download\" frees space on this device only — the Pi keeps the file for re-import. " +
                        "\"Delete everywhere\" also removes it from the Pi, for every device.",
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.delete(song); pendingDelete = null }) {
                        Text("Delete download")
                    }
                    TextButton(onClick = { vm.deleteEverywhere(song); pendingDelete = null }) {
                        Text("Delete everywhere", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    editing?.let { song ->
        var url by remember(song) { mutableStateOf("") }
        val videoId = YouTubeUrl.extractVideoId(url)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit artwork") },
            text = {
                Column {
                    Text("Paste a YouTube link — the artwork becomes that video's thumbnail.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        isError = url.isNotBlank() && videoId == null,
                        placeholder = { Text("https://youtu.be/…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (videoId != null) {
                        Spacer(Modifier.height(8.dp))
                        SongArtwork("https://i.ytimg.com/vi/$videoId/hqdefault.jpg", size = 96.dp)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = videoId != null, onClick = { vm.editArtwork(song, url); editing = null }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel") }
            },
        )
    }

    addingTo?.let { song ->
        AddToPlaylistSheet(songIds = listOf(song.id), onDismiss = { addingTo = null })
    }
}
