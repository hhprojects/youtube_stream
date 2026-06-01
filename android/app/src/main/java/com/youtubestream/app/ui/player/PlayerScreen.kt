package com.youtubestream.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.youtubestream.app.playback.AppRepeatMode
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.playback.PlayerUiState
import com.youtubestream.app.playback.QueueItem

@Composable
fun PlayerScreen(connection: PlaybackConnection, modifier: Modifier = Modifier) {
    val state by connection.state.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(connection) {
        connection.errors.collect {
            Toast.makeText(context, "Skipped a track — its file was missing (removed from library)", Toast.LENGTH_SHORT).show()
        }
    }

    if (!state.isConnected || state.currentMediaId == null) {
        EmptyPlayer(modifier) { connection.setQueueAndPlay(DebugTracks.TEST_TRACKS) }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val upNext = state.queue.drop(state.currentIndex + 1)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Artwork(
                state.artworkUri,
                Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        item {
            Text(
                state.title.ifBlank { "Unknown" },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item { Scrubber(state) { ms -> connection.seekTo(ms) } }
        item { Controls(state, connection) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Up next (${upNext.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    if (expanded) Icon(Icons.Filled.ExpandLess, contentDescription = "Collapse")
                    else Icon(Icons.Filled.ExpandMore, contentDescription = "Expand")
                }
            }
        }
        if (expanded) {
            items(upNext, key = { it.mediaId }) { item -> UpNextRow(item) }
        }
    }
}

@Composable
private fun EmptyPlayer(modifier: Modifier, onLoadTestTracks: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nothing playing")
        Button(onClick = onLoadTestTracks) { Text("Load test tracks") }
    }
}

@Composable
private fun Artwork(uri: String?, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall)
            Text(
                formatTime(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { connection.toggleShuffle() }) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (state.shuffleEnabled) active else idle)
        }
        IconButton(onClick = { connection.previous() }) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
        }
        IconButton(onClick = { connection.togglePlayPause() }) {
            if (state.isPlaying) Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(48.dp))
            else Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(48.dp))
        }
        IconButton(onClick = { connection.next() }) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next")
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
private fun UpNextRow(item: QueueItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
