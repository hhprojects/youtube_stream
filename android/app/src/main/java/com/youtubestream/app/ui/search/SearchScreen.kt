package com.youtubestream.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.network.allowsPiActions
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.ServerStatusBanner
import com.youtubestream.app.ui.playlist.AddToPlaylistSheet

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { SearchViewModel(it.searchRepository, it.downloadRepository, it.libraryRepository, it.serverReachability, it.recentSearchRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val downloaded by vm.downloadedIds.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onEnter() }
    var query by remember { mutableStateOf("") }
    var addingTo by remember { mutableStateOf<String?>(null) }   // library song id pending an add-to-playlist
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ServerStatusBanner(status, onRetry = { vm.onEnter() })
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search YouTube") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (status.allowsPiActions) vm.search(query)
                    focusManager.clearFocus()
                }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { vm.search(query) },
                enabled = status.allowsPiActions,
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Go") }
        }

        Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is UiState.Idle -> Text("Search for a song to begin.")
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${s.message}")
                    Button(onClick = { vm.search(query) }) { Text("Retry") }
                }
                is UiState.Content -> if (s.data.isEmpty()) {
                    Text("No results")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.data, key = { it.id }) { result ->
                            ResultRow(
                                result = result,
                                downloaded = result.id in downloaded,
                                download = downloads[result.id],
                                canDownload = status.allowsPiActions,
                                onDownload = { vm.download(result) },
                                onAddToPlaylist = { addingTo = result.id },
                            )
                        }
                    }
                }
            }
        }
    }

    addingTo?.let { id ->
        AddToPlaylistSheet(songId = id, onDismiss = { addingTo = null })
    }
}

@Composable
private fun ResultRow(
    result: SearchResult,
    downloaded: Boolean,
    download: ItemDownload?,
    canDownload: Boolean,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = result.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(result.title, maxLines = 1)
            Text(result.channel, maxLines = 1)
        }
        when {
            // Add-to-playlist is offered ONLY for already-downloaded results: a downloaded id is a real
            // library_songs.id, so the membership joins. A not-yet-downloaded id would be an invisible orphan.
            downloaded -> Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                }
                Icon(Icons.Filled.CheckCircle, contentDescription = "Downloaded")
            }
            download is ItemDownload.Downloading -> CircularProgressIndicator(
                progress = { download.fraction },
                modifier = Modifier.size(28.dp),
            )
            download is ItemDownload.Failed -> TextButton(onClick = onDownload, enabled = canDownload) { Text("Retry") }
            else -> TextButton(onClick = onDownload, enabled = canDownload) { Text("Download") }
        }
    }
}
