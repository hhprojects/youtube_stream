package com.youtubestream.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtubestream.app.playback.PlaybackController

/**
 * Persistent now-playing bar shown above the nav bar. Renders nothing until a track is loaded.
 * Because the controller is app-scoped, it stays correct across tab switches and configuration changes.
 */
@Composable
fun MiniPlayer(
    controller: PlaybackController,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    if (!state.isConnected || state.currentMediaId == null) return

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongArtwork(state.artworkUri, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.title.ifBlank { "Unknown" }, maxLines = 1)
                Text(state.artist, maxLines = 1)
            }
            IconButton(onClick = { controller.togglePlayPause() }) {
                if (state.isPlaying) Icon(Icons.Filled.Pause, contentDescription = "Pause")
                else Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
