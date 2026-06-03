package com.youtubestream.app.ui.imports

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel

@Composable
fun ImportScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel { ImportViewModel(it.piLibraryRepository, it.libraryRepository, it.downloadRepository) }
    val state by vm.state.collectAsState()
    val selected by vm.selected.collectAsState()
    val downloads by vm.downloads.collectAsState()
    var pendingDelete by remember { mutableStateOf<PiSong?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.errors.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            Text("Import from Pi", style = MaterialTheme.typography.titleLarge)
        }

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
                                onDelete = { pendingDelete = song },
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

    pendingDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete from the Pi?") },
            text = {
                Text(
                    "Permanently removes \"${song.title}\" from the Pi, for every device. " +
                        "You can re-download it from YouTube via Search.",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteFromPi(song); pendingDelete = null }) {
                    Text("Delete from Pi", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ImportRow(
    song: PiSong,
    checked: Boolean,
    progress: ImportItemState?,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
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
            is ImportItemState.Failed -> Text(
                progress.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            null -> {}
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete from Pi")
        }
    }
}
