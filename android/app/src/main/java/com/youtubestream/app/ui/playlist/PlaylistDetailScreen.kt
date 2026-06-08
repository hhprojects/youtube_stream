package com.youtubestream.app.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.data.util.YouTubeUrl
import com.youtubestream.app.playlist.PlaylistReorder
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.PlaylistCover
import com.youtubestream.app.ui.components.SelectionTopBar
import com.youtubestream.app.ui.components.SongArtwork
import com.youtubestream.app.ui.components.SongRow
import kotlin.math.roundToInt

@Composable
fun PlaylistDetailScreen(source: PlaylistSource, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel {
        PlaylistDetailViewModel(it.playlistRepository, it.playHistoryRepository, it.playbackConnection, source)
    }
    val editable = vm.isEditable
    val state by vm.state.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val closed by vm.closed.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingCover by remember { mutableStateOf(false) }
    var bulkRemoving by remember { mutableStateOf(false) }
    var bulkAdding by remember { mutableStateOf(false) }

    // The playlist was deleted (here or elsewhere) → leave the detail page. (Smart sources never close.)
    LaunchedEffect(closed) { if (closed) onBack() }
    BackHandler(enabled = selection.active) { vm.exitSelection() }

    val content = state as? UiState.Content

    // --- Drag-to-reorder state (manual playlists only) ---------------------------------------
    // `working` is the order the list renders. It's seeded from the DB list and re-seeded whenever
    // that list changes (remember(songs)). On drop we set `working` locally AND persist; when Room
    // re-emits the same order its key is equal, so `working` is kept — no flash back to the old order
    // in the gap before the DB round-trips. `displayed` is the live drag preview, produced by the
    // SAME pure fn that persists the order, so what you see is exactly what gets saved.
    val songs = content?.data?.songs.orEmpty()
    var working by remember(songs) { mutableStateOf(songs) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragFrom by remember { mutableIntStateOf(0) }
    var dragTo by remember { mutableIntStateOf(0) }
    var dragAccumPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val lazyState = rememberLazyListState()
    val spacingPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val displayed =
        if (draggedId == null) working
        else PlaylistReorder.reorder(working.map { it.id }, dragFrom, dragTo)
            .mapNotNull { id -> working.firstOrNull { it.id == id } }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        if (selection.active) {
            val ids = (content?.data?.songs).orEmpty().map { it.id }
            SelectionTopBar(
                count = selection.count,
                allSelected = selection.isAllSelected(ids),
                onClose = { vm.exitSelection() },
                onToggleSelectAll = { vm.toggleSelectAll(ids) },
            ) {
                IconButton(enabled = selection.count > 0, onClick = { bulkRemoving = true }) {
                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Remove from playlist")
                }
                IconButton(enabled = selection.count > 0, onClick = { bulkAdding = true }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to another playlist")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                }
                Text(
                    content?.data?.name ?: "Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (editable && content != null && content.data.songs.isNotEmpty()) {
                    TextButton(onClick = { vm.enterSelection() }) { Text("Select") }
                }
                // Overflow (rename / cover / delete) is for manual playlists only — smart ones are read-only.
                if (editable && content != null) {
                    Box {
                        var menu by remember { mutableStateOf(false) }
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; renaming = true })
                            DropdownMenuItem(text = { Text("Edit cover") }, onClick = { menu = false; editingCover = true })
                            if (content.data.coverArtUrl != null) {
                                DropdownMenuItem(
                                    text = { Text("Reset cover") },
                                    onClick = { menu = false; vm.setCover(null) },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete playlist") },
                                onClick = { menu = false; confirmDelete = true },
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
                is UiState.Content -> {
                    LazyColumn(
                        state = lazyState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Header(
                                data = s.data,
                                onPlay = { vm.play(displayed, 0) },
                                onShuffle = { vm.shuffle(displayed) },
                            )
                        }
                        if (displayed.isEmpty()) {
                            item {
                                Text(
                                    if (editable) {
                                        "No songs yet. Add songs from \"All songs\"."
                                    } else {
                                        "Songs you play show up here."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 16.dp),
                                )
                            }
                        } else {
                            itemsIndexed(displayed, key = { _, song -> song.id }) { index, song ->
                                val isDragged = song.id == draggedId
                                // The dragged row follows the finger via translationY; the OTHERS reflow
                                // with animateItem(). Never both on one row — they'd fight and jitter.
                                // Smart (read-only) rows don't reorder, so they get a plain modifier.
                                val rowModifier =
                                    when {
                                        isDragged -> Modifier
                                            .zIndex(1f)
                                            .graphicsLayer {
                                                translationY =
                                                    (dragFrom - dragTo) * (rowHeightPx + spacingPx).toFloat() + dragAccumPx
                                                scaleX = 1.03f
                                                scaleY = 1.03f
                                            }
                                        editable -> Modifier.animateItem()
                                        else -> Modifier
                                    }
                                SongRow(
                                    title = song.title,
                                    artist = song.artist,
                                    artworkUrl = song.artworkUrl,
                                    inSelectionMode = selection.active,
                                    selected = song.id in selection.selectedIds,
                                    onClick = {
                                        if (selection.active) vm.toggle(song.id) else vm.play(displayed, index)
                                    },
                                    modifier = rowModifier.onSizeChanged { if (it.height > 0) rowHeightPx = it.height },
                                    trailing = {
                                        // Edit controls render for manual playlists only; smart rows are play-only.
                                        if (editable) {
                                            IconButton(onClick = { vm.removeSong(song.id) }) {
                                                Icon(
                                                    Icons.Filled.RemoveCircleOutline,
                                                    contentDescription = "Remove from playlist",
                                                )
                                            }
                                            // Long-press the handle (not the whole row, which taps to play) to drag.
                                            Box(
                                                Modifier
                                                    .size(40.dp)
                                                    .pointerInput(song.id) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                draggedId = song.id
                                                                dragFrom = working.indexOfFirst { it.id == song.id }
                                                                dragTo = dragFrom
                                                                dragAccumPx = 0f
                                                            },
                                                            onDrag = { change, amount ->
                                                                change.consume()
                                                                dragAccumPx += amount.y
                                                                // Stride = row height + the 8dp spacedBy gap, or the
                                                                // threshold drifts ~8dp/row over a long list.
                                                                val stride = rowHeightPx + spacingPx
                                                                if (stride > 0) {
                                                                    dragTo = (dragFrom + (dragAccumPx / stride).roundToInt())
                                                                        .coerceIn(0, working.lastIndex)
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val finalIds = PlaylistReorder.reorder(
                                                                    working.map { it.id }, dragFrom, dragTo,
                                                                )
                                                                working = finalIds.mapNotNull { id ->
                                                                    working.firstOrNull { it.id == id }
                                                                }
                                                                vm.reorder(finalIds)
                                                                draggedId = null
                                                            },
                                                            onDragCancel = { draggedId = null },
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Filled.DragHandle, contentDescription = "Reorder")
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        val current = content?.data?.name.orEmpty()
        RenameDialog(
            current = current,
            onConfirm = { name -> vm.rename(name); renaming = false },
            onDismiss = { renaming = false },
        )
    }

    if (editingCover) {
        EditCoverDialog(
            onConfirm = { coverUrl -> vm.setCover(coverUrl); editingCover = false },
            onDismiss = { editingCover = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this playlist?") },
            text = { Text("The playlist is removed. Your songs stay in the library.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(); confirmDelete = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    if (bulkRemoving) {
        AlertDialog(
            onDismissRequest = { bulkRemoving = false },
            title = { Text(if (selection.count == 1) "Remove 1 song from this playlist?" else "Remove ${selection.count} songs from this playlist?") },
            text = { Text("The songs stay in your library — they're only removed from this playlist.") },
            confirmButton = {
                TextButton(onClick = { vm.removeSelected(); bulkRemoving = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { bulkRemoving = false }) { Text("Cancel") }
            },
        )
    }

    // AddToPlaylistSheet is in this package — no import needed. Selection is retained after add; ✕ exits.
    if (bulkAdding) {
        AddToPlaylistSheet(songIds = selection.selectedIds.toList(), onDismiss = { bulkAdding = false })
    }
}

@Composable
private fun Header(data: PlaylistDetailData, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlaylistCover(data.coverArtUrl, data.songs.firstOrNull()?.artworkUrl, size = 160.dp)
        Spacer(Modifier.padding(top = 6.dp))
        Text(
            data.name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${data.songs.size} songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlay, enabled = data.songs.isNotEmpty()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Play")
            }
            FilledTonalButton(onClick = onShuffle, enabled = data.songs.isNotEmpty()) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Shuffle")
            }
        }
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Custom playlist cover. Same paste-a-YouTube-link → thumbnail flow as the song-artwork editor
 * (consistent UX, and the ytimg CDN renders even when the emulator's DNS is flaky). The cover is
 * stored as that thumbnail URL; "Reset cover" (in the overflow) clears it back to the first-song art.
 */
@Composable
private fun EditCoverDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    val videoId = YouTubeUrl.extractVideoId(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit cover") },
        text = {
            Column {
                Text("Paste a YouTube link — the cover becomes that video's thumbnail.")
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
            TextButton(
                enabled = videoId != null,
                onClick = { onConfirm("https://i.ytimg.com/vi/$videoId/hqdefault.jpg") },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
