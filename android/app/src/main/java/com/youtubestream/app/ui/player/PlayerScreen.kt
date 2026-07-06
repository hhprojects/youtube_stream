package com.youtubestream.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import com.youtubestream.app.lyrics.SongRef
import com.youtubestream.app.playback.AppRepeatMode
import com.youtubestream.app.playback.formatSpeed
import com.youtubestream.app.playback.nextPlaybackSpeed
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.playback.PlayerUiState
import com.youtubestream.app.playback.QueueItem
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.playlist.AddToPlaylistSheet

@Composable
fun PlayerScreen(
    connection: PlaybackConnection,
    onMinimize: () -> Unit,
    onStop: () -> Unit,
    onBrowseLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by connection.state.collectAsStateWithLifecycle()

    if (!state.isConnected || state.currentMediaId == null) {
        EmptyPlayer(modifier, onBrowseLibrary)
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var addingToId by remember { mutableStateOf<String?>(null) }   // current track's id, pending add-to-playlist
    var sleepDialogOpen by remember { mutableStateOf(false) }
    val sleepActive = state.sleepTimerEndsAtMs != null || state.sleepAtTrackEnd
    // Up-next in TIMELINE order — which is now the TRUE play order (shuffle is app-managed in the
    // timeline; Media3's hidden shuffle permutation is never used). Two tiers over one index space:
    // the manual "Next in queue" block first, then the "Next up" context; rows keep their absolute
    // timeline index so play/remove/move are identical for both sections.
    val upNext = state.queue.drop(state.currentIndex + 1)

    // Drag-reorder state. `working` mirrors up-next but is locally reorderable; it re-seeds only when the
    // real queue/current item changes — NOT on the 500ms position tick (state.queue keeps its identity
    // across the .copy()). Index-based (the queue may hold duplicate mediaIds). The dragged row floats via
    // translationY while the list stays put; on release the new order commits to the controller.
    var working by remember(state.queue, state.currentIndex) { mutableStateOf(upNext) }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragAccumPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val stridePx = rowHeightPx + with(LocalDensity.current) { 16.dp.roundToPx() } // row + the spacedBy(16.dp) gap

    // Section boundary, derived from the locally reorderable list so it tracks in-flight drags.
    val pendingCount = working.takeWhile { it.isManual }.count()

    var showLyrics by remember { mutableStateOf(false) }
    // Lyrics are songs-only; currentMediaId is non-null here (guarded by the early return above).
    val canShowLyrics = !state.isPodcast
    val lyricsVm = appViewModel { c -> LyricsViewModel(c.lyricsRepository) }
    val lyrics by lyricsVm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.currentMediaId, state.isPodcast, state.title, state.artist) {
        val id = state.currentMediaId
        if (id != null && !state.isPodcast) {
            lyricsVm.load(SongRef(id, state.title, state.artist, state.durationMs))
        }
    }
    val lyricsMode = showLyrics && canShowLyrics

    // Solid base so the sheet is never see-through: songs without artwork show this surface; songs with
    // art show the blurred backdrop (opaque) on top of it. A Surface (not a bare Box) is essential here:
    // it sets LocalContentColor to onSurface, so the un-tinted Text/Icons below stay visible in dark mode.
    // The player is drawn outside the Scaffold, so nothing else supplies a content color — a Box would
    // leave them at the default black and they'd vanish on the dark surface (the MiniPlayer uses a Surface
    // for the same reason). Surface stacks its children in an internal fill box, so the layout is unchanged.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // Immersive backdrop: the current art, blown up + blurred, tinted by a scrim for legibility.
        ArtBackdrop(state.artworkUri)

        if (lyricsMode) {
            // Bounded Column: lyrics own their scroll (weight(1f)); transport stays pinned below. A
            // scrolling LazyColumn nested in the outer LazyColumn item would crash (infinite max height).
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
                PlayerHeader(
                    onMinimize = onMinimize,
                    onStop = onStop,
                    onAddToPlaylist = { state.currentMediaId?.let { addingToId = it } },
                    sleepActive = sleepActive,
                    onSleepTimer = { sleepDialogOpen = true },
                    canShowLyrics = canShowLyrics,
                    showLyrics = true,
                    onToggleLyrics = { showLyrics = it },
                )
                LyricsView(lyrics, state.positionMs, Modifier.weight(1f))
                TrackInfo(state.title, state.artist)
                Scrubber(state) { ms -> connection.seekTo(ms) }
                Controls(state, connection)
            }
        } else {
            val queueRow: @Composable (Int, QueueItem) -> Unit = { wi, item ->
                val absoluteIndex = state.currentIndex + 1 + wi
                UpNextRow(
                    item = item,
                    dragged = dragFrom == wi,
                    dragOffsetPx = if (dragFrom == wi) dragAccumPx else 0f,
                    canMoveUp = wi > 0,
                    canMoveDown = wi < working.lastIndex,
                    onMeasured = { h -> if (h > 0) rowHeightPx = h },
                    onPlay = { connection.playQueueItem(absoluteIndex) },
                    onRemove = { connection.removeQueueItem(absoluteIndex) },
                    onMoveUp = {
                        working = moveItem(working, wi, wi - 1)
                        connection.moveQueueItem(absoluteIndex, absoluteIndex - 1)
                    },
                    onMoveDown = {
                        working = moveItem(working, wi, wi + 1)
                        connection.moveQueueItem(absoluteIndex, absoluteIndex + 1)
                    },
                    onDragStart = { dragFrom = wi; dragAccumPx = 0f },
                    onDrag = { dy -> dragAccumPx += dy },
                    onDragEnd = {
                        val from = dragFrom
                        if (from != null && stridePx > 0) {
                            val to = (from + (dragAccumPx / stridePx).roundToInt())
                                .coerceIn(0, working.lastIndex)
                            if (to != from) {
                                working = moveItem(working, from, to)
                                connection.moveQueueItem(
                                    state.currentIndex + 1 + from,
                                    state.currentIndex + 1 + to,
                                )
                            }
                        }
                        dragFrom = null
                    },
                    onDragCancel = { dragFrom = null },
                )
            }
            LazyColumn(
                // systemBarsPadding keeps content (chevron, art, controls, queue) inside the safe area —
                // the ArtBackdrop above stays full-bleed under the status/nav bars for the immersive look.
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PlayerHeader(
                        onMinimize = onMinimize,
                        onStop = onStop,
                        onAddToPlaylist = { state.currentMediaId?.let { addingToId = it } },
                        sleepActive = sleepActive,
                        onSleepTimer = { sleepDialogOpen = true },
                        canShowLyrics = canShowLyrics,
                        showLyrics = false,
                        onToggleLyrics = { showLyrics = it },
                    )
                }
                item { HeroArtwork(state.artworkUri, Modifier.fillMaxWidth().aspectRatio(1f)) }
                item { TrackInfo(state.title, state.artist) }
                item { Scrubber(state) { ms -> connection.seekTo(ms) } }
                item { Controls(state, connection) }
                item {
                    UpNextHeader(
                        count = working.size,
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                    )
                }
                if (expanded) {
                    // Keys stay position-based (duplicates legal) but get a section prefix so header
                    // items never collide with row keys.
                    if (pendingCount > 0) {
                        item(key = "queue-header") {
                            QueueSectionHeader("Next in queue ($pendingCount)") { connection.clearManualQueue() }
                        }
                    }
                    itemsIndexed(working.take(pendingCount), key = { i, _ -> "q${state.currentIndex + 1 + i}" }) { i, item ->
                        queueRow(i, item)
                    }
                    if (working.size > pendingCount) {
                        item(key = "ctx-header") {
                            QueueSectionHeader("Next up (${working.size - pendingCount})") { connection.clearUpNext() }
                        }
                    }
                    itemsIndexed(working.drop(pendingCount), key = { i, _ -> "c${state.currentIndex + 1 + pendingCount + i}" }) { i, item ->
                        queueRow(pendingCount + i, item)
                    }
                }
            }
        }
    }

    addingToId?.let { id ->
        AddToPlaylistSheet(songIds = listOf(id), onDismiss = { addingToId = null })
    }

    if (sleepDialogOpen) {
        SleepTimerDialog(state, connection, onDismiss = { sleepDialogOpen = false })
    }
}

/** Header for the expanded player: minimize chevron (left) + a songs-only Art/Lyrics toggle + overflow ⋮. */
@Composable
private fun PlayerHeader(
    onMinimize: () -> Unit,
    onStop: () -> Unit,
    onAddToPlaylist: () -> Unit,
    sleepActive: Boolean,
    onSleepTimer: () -> Unit,
    canShowLyrics: Boolean,
    showLyrics: Boolean,
    onToggleLyrics: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinimize) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize")
        }
        Spacer(Modifier.weight(1f))
        if (canShowLyrics) {
            IconToggleButton(checked = showLyrics, onCheckedChange = onToggleLyrics) {
                Icon(
                    Icons.Filled.Lyrics,
                    contentDescription = if (showLyrics) "Show artwork" else "Show lyrics",
                    tint = if (showLyrics) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    onClick = { menuOpen = false; onAddToPlaylist() },
                )
                DropdownMenuItem(
                    text = { Text(if (sleepActive) "Sleep timer · on" else "Sleep timer") },
                    onClick = { menuOpen = false; onSleepTimer() },
                )
                DropdownMenuItem(
                    text = { Text("Stop") },
                    onClick = { menuOpen = false; onStop() },
                )
            }
        }
    }
}

/** Sleep-timer picker: timed durations, "End of track", and Off (the currently-active option is checked). */
@Composable
private fun SleepTimerDialog(
    state: PlayerUiState,
    connection: PlaybackConnection,
    onDismiss: () -> Unit,
) {
    val active = state.sleepTimerEndsAtMs != null || state.sleepAtTrackEnd
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                listOf(15, 30, 45, 60).forEach { min ->
                    DropdownMenuItem(
                        text = { Text("$min minutes") },
                        onClick = { connection.setSleepTimer(min * 60_000L); onDismiss() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("End of track" + if (state.sleepAtTrackEnd) "  ✓" else "") },
                    onClick = { connection.setSleepTimerEndOfTrack(); onDismiss() },
                )
                if (active) {
                    DropdownMenuItem(
                        text = { Text("Turn off", color = MaterialTheme.colorScheme.error) },
                        onClick = { connection.cancelSleepTimer(); onDismiss() },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Full-bleed album art, scaled up and blurred, with a surface scrim so foreground text stays legible. */
@Composable
private fun ArtBackdrop(uri: String?) {
    Box(Modifier.fillMaxSize()) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(64.dp),
            )
        }
        // Lighter at the top (let the art show), heavier toward the bottom where the controls sit.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    ),
                ),
            ),
        )
    }
}

/** Large rounded album art with a music-note fallback (square-cropped so 16:9 thumbnails don't letterbox). */
@Composable
private fun HeroArtwork(uri: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 12.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Fallback note sits *behind* the image, so a still-loading or failed fetch isn't a blank box.
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TrackInfo(title: String, artist: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            title.ifBlank { "Unknown" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )
        Text(
            artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Scrubber(state: PlayerUiState, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val dur = state.durationMs
    val fraction = if (dur > 0) (state.positionMs.toFloat() / dur).coerceIn(0f, 1f) else 0f

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = if (dragging) dragValue else fraction,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                if (dur > 0) onSeek((dragValue * dur).toLong())
                dragging = false
            },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelMedium)
            Text(
                formatTime(state.durationMs),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun Controls(state: PlayerUiState, connection: PlaybackConnection) {
    if (state.isPodcast) EpisodeControls(state, connection) else SongControls(state, connection)
}

@Composable
private fun SongControls(state: PlayerUiState, connection: PlaybackConnection) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { connection.toggleShuffle() },
            // State is otherwise conveyed only by tint; stateDescription makes it audible to TalkBack.
            modifier = Modifier.semantics { stateDescription = if (state.shuffleEnabled) "On" else "Off" },
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (state.shuffleEnabled) active else idle)
        }
        IconButton(onClick = { connection.previous() }) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
        }
        // The hero control: a large filled button that picks up the expressive theme's shape + spring motion.
        FilledIconButton(onClick = { connection.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
            if (state.isPlaying) Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(36.dp))
            else Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = { connection.next() }) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = { connection.cycleRepeat() }) {
            when (state.repeatMode) {
                AppRepeatMode.OFF -> Icon(Icons.Filled.Repeat, contentDescription = "Repeat off", tint = idle)
                AppRepeatMode.QUEUE -> Icon(Icons.Filled.Repeat, contentDescription = "Repeat queue", tint = active)
                AppRepeatMode.TRACK -> Icon(Icons.Filled.RepeatOne, contentDescription = "Repeat track", tint = active)
            }
        }
    }
}

/** Episodes: speed toggle + skip −15s/+30s replace shuffle/prev/next/repeat (meaningless on one episode). */
@Composable
private fun EpisodeControls(state: PlayerUiState, connection: PlaybackConnection) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { connection.setSpeed(nextPlaybackSpeed(state.playbackSpeed)) }) {
            Text(formatSpeed(state.playbackSpeed), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = { connection.seekBy(-15_000) }) {
            Icon(Icons.Filled.FastRewind, contentDescription = "Skip back 15 seconds", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = { connection.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
            if (state.isPlaying) Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(36.dp))
            else Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = { connection.seekBy(30_000) }) {
            Icon(Icons.Filled.FastForward, contentDescription = "Skip forward 30 seconds", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun UpNextHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Up next ($count)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onToggle) {
            if (expanded) Icon(Icons.Filled.ExpandLess, contentDescription = "Collapse")
            else Icon(Icons.Filled.ExpandMore, contentDescription = "Expand")
        }
    }
}

/** Queue section subheader: tier label + its own Clear (queue and context clear independently). */
@Composable
private fun QueueSectionHeader(label: String, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) { Text("Clear") }
    }
}

/**
 * A queue row: tap the body to jump to that track; the trailing ✕ removes it; long-press the ☰ handle
 * (not the row, which taps to play) to drag-reorder. While [dragged], the row floats via [dragOffsetPx]
 * (translationY) above the others; the list itself stays put until the drop commits the new order.
 */
@Composable
private fun UpNextRow(
    item: QueueItem,
    dragged: Boolean,
    dragOffsetPx: Float,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMeasured: (Int) -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .zIndex(if (dragged) 1f else 0f)
            .graphicsLayer {
                if (dragged) {
                    translationY = dragOffsetPx
                    scaleX = 1.03f
                    scaleY = 1.03f
                }
            }
            .clickable(onClick = onPlay)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove from queue")
        }
        Box(
            Modifier
                .size(48.dp)   // 48dp min touch target
                // Drag is touch-only; expose discrete Move up/down so TalkBack & Switch Access can reorder.
                .semantics {
                    customActions = buildList {
                        if (canMoveUp) add(CustomAccessibilityAction("Move up") { onMoveUp(); true })
                        if (canMoveDown) add(CustomAccessibilityAction("Move down") { onMoveDown(); true })
                    }
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.DragHandle, contentDescription = "Reorder")
        }
    }
}

@Composable
private fun EmptyPlayer(modifier: Modifier, onBrowseLibrary: () -> Unit) {
    // Same reasoning as the main player body: a Surface (not a bare Column) makes this opaque AND sets
    // LocalContentColor to onSurface, so the "Nothing playing" title stays visible in dark mode instead
    // of falling back to the default black. Reachable via the widget's open-player deep link when nothing
    // is loaded, so it's a real path, not just theoretical.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("Nothing playing", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pick a song from your Library to start playing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onBrowseLibrary) { Text("Go to Library") }
        }
    }
}
