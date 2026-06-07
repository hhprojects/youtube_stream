package com.youtubestream.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.youtubestream.app.playback.AppRepeatMode
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.playback.PlayerUiState
import com.youtubestream.app.playback.QueueItem

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
    val upNext = state.queue.drop(state.currentIndex + 1)

    // Solid base so the sheet is never see-through: songs without artwork show this surface; songs with
    // art show the blurred backdrop (opaque) on top of it.
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Immersive backdrop: the current art, blown up + blurred, tinted by a scrim for legibility.
        ArtBackdrop(state.artworkUri)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PlayerHeader(onMinimize = onMinimize, onStop = onStop) }
            item { HeroArtwork(state.artworkUri, Modifier.fillMaxWidth().aspectRatio(1f)) }
            item { TrackInfo(state.title, state.artist) }
            item { Scrubber(state) { ms -> connection.seekTo(ms) } }
            item { Controls(state, connection) }
            item { UpNextHeader(upNext.size, expanded) { expanded = !expanded } }
            if (expanded) items(upNext, key = { it.mediaId }) { item -> UpNextRow(item) }
        }
    }
}

/** Header for the expanded player: minimize chevron (left) + overflow ⋮ with Stop (right). */
@Composable
private fun PlayerHeader(onMinimize: () -> Unit, onStop: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinimize) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize")
        }
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Stop") },
                    onClick = { menuOpen = false; onStop() },
                )
            }
        }
    }
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
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { connection.toggleShuffle() }) {
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

@Composable
private fun UpNextRow(item: QueueItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyPlayer(modifier: Modifier, onBrowseLibrary: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
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
