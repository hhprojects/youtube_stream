package com.youtubestream.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.data.local.PlaylistSummary
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.PlaylistCover

/**
 * Playlists-first Library landing. The existing flat all-songs list is now reached through the
 * pinned "All songs" row; tapping a playlist opens its detail page. Smart playlists arrive in a
 * later slice (they'd be pinned here next to "All songs").
 */
@Composable
fun LibraryHomeScreen(
    onOpenAllSongs: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel { LibraryHomeViewModel(it.playlistRepository, it.libraryRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${s.message}")
            is UiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                item {
                    PinnedRow(
                        icon = Icons.Filled.LibraryMusic,
                        title = "All songs",
                        subtitle = "${s.data.allSongsCount} songs",
                        onClick = onOpenAllSongs,
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Your playlists",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { creating = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("New")
                        }
                    }
                }
                if (s.data.playlists.isEmpty()) {
                    item {
                        Text(
                            "No playlists yet. Tap \"New\" to create one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(s.data.playlists, key = { it.id }) { playlist ->
                        PlaylistRow(playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }
    }

    if (creating) {
        NewPlaylistDialog(
            onConfirm = { name -> vm.createPlaylist(name); creating = false },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun PinnedRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(summary: PlaylistSummary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistCover(summary.coverArtUrl, summary.firstArtworkUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(summary.name, maxLines = 1)
                Text(
                    "${summary.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewPlaylistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
