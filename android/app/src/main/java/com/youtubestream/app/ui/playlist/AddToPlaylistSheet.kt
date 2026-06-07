package com.youtubestream.app.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.PlaylistCover

/**
 * Bottom-sheet picker: add the song with id [songId] to an existing playlist or create a new one.
 * Takes just the id (not a full LibrarySong) because that's all it needs — which is what lets the
 * same sheet be opened from Library, Search, and Now-Playing. Callers must pass a *library* song id
 * (`library_songs.id`): a non-library id would create an invisible orphan membership (no JOIN match).
 * Adding a duplicate is a silent no-op (DAO IGNORE).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(songId: String, onDismiss: () -> Unit) {
    val vm = appViewModel { AddToPlaylistViewModel(it.playlistRepository) }
    val summaries by vm.summaries.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            PickerRow(icon = { Icon(Icons.Filled.Add, contentDescription = null) }, title = "New playlist") {
                creating = true
            }
            summaries.forEach { playlist ->
                PickerRow(
                    icon = { PlaylistCover(playlist.coverArtUrl, playlist.firstArtworkUrl, size = 40.dp) },
                    title = playlist.name,
                    subtitle = "${playlist.songCount} songs",
                ) {
                    vm.add(playlist.id, songId)
                    onDismiss()
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Playlist name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { vm.createAndAdd(name, songId); creating = false; onDismiss() },
                ) { Text("Create & add") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PickerRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
