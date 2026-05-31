package com.youtubestream.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtubestream.app.playback.PlayableTrack
import com.youtubestream.app.playback.PlaybackConnection

/** Public-domain test audio so the screen works before the backend is wired. */
private val TEST_TRACKS = listOf(
    PlayableTrack(
        mediaId = "t1",
        uri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
        title = "Test Track 1",
        artist = "Sevish",
    ),
    PlayableTrack(
        mediaId = "t2",
        uri = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
        title = "Test Track 2",
        artist = "Epoq",
    ),
)

@Composable
fun DebugPlaybackScreen(
    connection: PlaybackConnection,
    modifier: Modifier = Modifier,
) {
    val state by connection.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("connected=${state.isConnected}  playing=${state.isPlaying}")
        Text("now: ${state.title} — ${state.artist}")
        Text("pos=${state.positionMs}ms / dur=${state.durationMs}ms")
        Text("repeat=${state.repeatMode}  shuffle=${state.shuffleEnabled}")

        Button(onClick = { connection.setQueueAndPlay(TEST_TRACKS) }) { Text("Load queue & play") }
        Button(onClick = { connection.togglePlayPause() }) { Text("Play / Pause") }
        Button(onClick = { connection.previous() }) { Text("Previous") }
        Button(onClick = { connection.next() }) { Text("Next") }
        Button(onClick = { connection.toggleShuffle() }) { Text("Toggle shuffle") }
        Button(onClick = { connection.cycleRepeat() }) { Text("Cycle repeat") }
    }
}
