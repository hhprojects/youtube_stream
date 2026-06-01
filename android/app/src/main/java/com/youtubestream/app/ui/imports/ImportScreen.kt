package com.youtubestream.app.ui.imports

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel

@Composable
fun ImportScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { ImportViewModel(it.piLibraryRepository, it.libraryRepository, it.downloadRepository) }
    val state by vm.state.collectAsState()
    val selected by vm.selected.collectAsState()
    val downloads by vm.downloads.collectAsState()

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Import from Pi", style = MaterialTheme.typography.titleLarge)

        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${s.message}")
                    Button(onClick = { vm.refresh() }) { Text("Retry") }
                }
                is UiState.Content -> if (s.data.isEmpty()) {
                    Text("Everything on the Pi is already on this device.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(s.data, key = { it.id }) { song ->
                            ImportRow(
                                song = song,
                                checked = song.id in selected,
                                progress = downloads[song.id],
                                onToggle = { vm.toggle(song.id) },
                            )
                        }
                    }
                }
            }
        }

        val s = state
        if (s is UiState.Content && s.data.isNotEmpty()) {
            Button(
                onClick = { vm.downloadSelected(s.data) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selected.isEmpty()) "Select songs to import" else "Download ${selected.size}")
            }
        }
    }
}

@Composable
private fun ImportRow(
    song: PiSong,
    checked: Boolean,
    progress: ImportItemState?,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when (progress) {
            is ImportItemState.Downloading -> CircularProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.size(24.dp),
            )
            is ImportItemState.Failed -> Text("Failed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            null -> {}
        }
    }
}
