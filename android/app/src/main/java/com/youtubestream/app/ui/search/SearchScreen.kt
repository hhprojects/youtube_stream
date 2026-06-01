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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import coil3.compose.AsyncImage
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { SearchViewModel(it.searchRepository, it.downloadRepository, it.libraryRepository) }
    val state by vm.state.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val downloaded by vm.downloadedIds.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search YouTube") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { vm.search(query) }, modifier = Modifier.padding(start = 8.dp)) { Text("Go") }
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
                                onDownload = { vm.download(result) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    result: SearchResult,
    downloaded: Boolean,
    download: ItemDownload?,
    onDownload: () -> Unit,
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
            downloaded -> Icon(Icons.Filled.CheckCircle, contentDescription = "Downloaded")
            download is ItemDownload.Downloading -> CircularProgressIndicator(
                progress = { download.fraction },
                modifier = Modifier.size(28.dp),
            )
            download is ItemDownload.Failed -> TextButton(onClick = onDownload) { Text("Retry") }
            else -> TextButton(onClick = onDownload) { Text("Download") }
        }
    }
}
