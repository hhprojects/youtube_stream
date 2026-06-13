package com.youtubestream.app.ui.podcast

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SelectionTopBar
import com.youtubestream.app.ui.components.SongRow

/** Global list of every downloaded episode (all shows): play, single-delete, or multi-select bulk-delete. */
@Composable
fun DownloadedEpisodesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel { c ->
        // Resume-aware play, identical to ShowDetail/PodcastHome: finished episodes restart from 0.
        val play: (PodcastEpisode) -> Unit = { ep ->
            c.playbackConnection.setQueueAndPlay(
                listOf(ep.toPlayableTrack()), 0, if (ep.isFinished) 0L else ep.resumePositionMs,
            )
        }
        DownloadedEpisodesViewModel(repo = c.podcastRepository, play = play)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<PodcastEpisode?>(null) }
    var bulkDeleting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.errors.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    // While selecting, Back exits the mode instead of leaving the screen.
    BackHandler(enabled = selection.active) { vm.exitSelection() }

    val episodes = (state as? UiState.Content)?.data.orEmpty()
    val episodeIds = episodes.map { it.id }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        if (selection.active) {
            SelectionTopBar(
                count = selection.count,
                allSelected = selection.isAllSelected(episodeIds),
                onClose = { vm.exitSelection() },
                onToggleSelectAll = { vm.toggleSelectAll(episodeIds) },
            ) {
                IconButton(enabled = selection.count > 0, onClick = { bulkDeleting = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                }
                Text("Downloaded episodes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (episodes.isNotEmpty()) {
                    TextButton(onClick = { vm.enterSelection() }) { Text("Select") }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text("Error: ${s.message}")
                is UiState.Content -> if (s.data.isEmpty()) {
                    Text("No downloaded episodes yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.data, key = { it.id }) { ep ->
                            SongRow(
                                title = ep.title,
                                artist = ep.showName,
                                artworkUrl = ep.artworkUrl,
                                inSelectionMode = selection.active,
                                selected = ep.id in selection.selectedIds,
                                onClick = {
                                    if (selection.active) vm.toggle(ep.id) else vm.onPlayDownloaded(ep.id)
                                },
                                onLongClick = { if (!selection.active) vm.enterSelection(ep.id) },
                                trailing = {
                                    IconButton(onClick = { pendingDelete = ep }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete download")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { ep ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${ep.title}\"?") },
            text = {
                Text(
                    "\"Delete download\" frees space on this device only — the Pi keeps the file. " +
                        "\"Delete everywhere\" also removes it from the Pi.",
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.onDeleteDownload(ep.id); pendingDelete = null }) {
                        Text("Delete download")
                    }
                    TextButton(onClick = { vm.onDeleteEverywhere(ep.id); pendingDelete = null }) {
                        Text("Delete everywhere", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (bulkDeleting) {
        val targetCount = episodes.count { it.id in selection.selectedIds }
        AlertDialog(
            onDismissRequest = { bulkDeleting = false },
            title = { Text(if (targetCount == 1) "Delete 1 episode?" else "Delete $targetCount episodes?") },
            text = {
                Text(
                    "\"Delete downloads\" frees space on this device only — the Pi keeps the files. " +
                        "\"Delete everywhere\" also removes them from the Pi.",
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.deleteSelectedDownloads(episodes); bulkDeleting = false }) {
                        Text("Delete downloads")
                    }
                    TextButton(onClick = { vm.deleteSelectedEverywhere(episodes); bulkDeleting = false }) {
                        Text("Delete everywhere", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { bulkDeleting = false }) { Text("Cancel") }
            },
        )
    }
}
