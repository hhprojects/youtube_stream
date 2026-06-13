package com.youtubestream.app.ui.library

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.util.YouTubeUrl
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SelectionTopBar
import com.youtubestream.app.ui.components.SongArtwork
import com.youtubestream.app.ui.components.SongRow
import com.youtubestream.app.ui.playlist.AddToPlaylistSheet

@Composable
fun LibraryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel { LibraryViewModel(it.libraryRepository, it.piLibraryRepository, it.playbackConnection) }
    val state by vm.state.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<LibrarySong?>(null) }
    var editing by remember { mutableStateOf<LibrarySong?>(null) }
    var addingTo by remember { mutableStateOf<LibrarySong?>(null) }
    var bulkAdding by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.errors.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    // While selecting, Back exits the mode instead of leaving the screen.
    BackHandler(enabled = selection.active) { vm.exitSelection() }

    val songs = (state as? UiState.Content)?.data.orEmpty()

    var sort by remember { mutableStateOf(LibrarySort.RECENTLY_ADDED) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    // The visible list: filtered, then sorted. Play / select-all / row-index all operate on THIS list,
    // so tapping the 3rd visible row plays the 3rd visible row even when sorted or filtered.
    val displayed = remember(songs, sort, query) { filterLibrary(sortLibrary(songs, sort), query) }
    val displayedIds = displayed.map { it.id }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        if (selection.active) {
            SelectionTopBar(
                count = selection.count,
                allSelected = selection.isAllSelected(displayedIds),
                onClose = { vm.exitSelection() },
                onToggleSelectAll = { vm.toggleSelectAll(displayedIds) },
            ) {
                IconButton(enabled = selection.count > 0, onClick = { bulkAdding = true }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                }
                IconButton(enabled = selection.count > 0, onClick = { bulkDeleting = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                }
                Text("All songs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (songs.isNotEmpty()) {
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search songs")
                    }
                    TextButton(onClick = { vm.enterSelection() }) { Text("Select") }
                }
            }
        }

        // Play / Shuffle the whole (filtered) list + a sort menu — shown outside selection when there's content.
        if (!selection.active && songs.isNotEmpty()) {
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Filter songs") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { vm.play(displayed, 0) }, enabled = displayed.isNotEmpty()) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { vm.playShuffled(displayed) }, enabled = displayed.isNotEmpty()) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Shuffle")
                }
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { sortMenuOpen = true }) { Text(sort.label) }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        LibrarySort.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = { sort = opt; sortMenuOpen = false },
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text("Error: ${s.message}")
                is UiState.Content -> if (s.data.isEmpty()) {
                    Text("No downloads yet. Search for a song and tap Download.")
                } else if (displayed.isEmpty()) {
                    Text("No songs match \"${query.trim()}\".")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(displayed, key = { _, song -> song.id }) { index, song ->
                            SongRow(
                                title = song.title,
                                artist = song.artist,
                                artworkUrl = song.artworkUrl,
                                inSelectionMode = selection.active,
                                selected = song.id in selection.selectedIds,
                                onClick = {
                                    if (selection.active) vm.toggle(song.id) else vm.play(displayed, index)
                                },
                                onLongClick = { if (!selection.active) vm.enterSelection(song.id) },
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

    if (bulkDeleting) {
        val targetCount = songs.count { it.id in selection.selectedIds }
        AlertDialog(
            onDismissRequest = { bulkDeleting = false },
            title = { Text(if (targetCount == 1) "Delete 1 song?" else "Delete $targetCount songs?") },
            text = {
                Text(
                    "\"Delete downloads\" frees space on this device only — the Pi keeps the files for re-import. " +
                        "\"Delete everywhere\" also removes them from the Pi, for every device.",
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.deleteSelectedDownloads(songs); bulkDeleting = false }) {
                        Text("Delete downloads")
                    }
                    TextButton(onClick = { vm.deleteSelectedEverywhere(songs); bulkDeleting = false }) {
                        Text("Delete everywhere", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { bulkDeleting = false }) { Text("Cancel") }
            },
        )
    }

    // Selection is retained after a bulk add (so the same set can go to several playlists); ✕ exits.
    if (bulkAdding) {
        AddToPlaylistSheet(songIds = selection.selectedIds.toList(), onDismiss = { bulkAdding = false })
    }
}
