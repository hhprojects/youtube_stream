package com.youtubestream.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** One simple track description the UI can hand to the player. */
data class PlayableTrack(
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
)

/**
 * The UI's only door to playback. Connects a [MediaController] to [PlaybackService],
 * mirrors player events into [state], and exposes control methods. Main-thread-confined.
 */
class PlaybackConnection(
    private val context: Context,
    private val scope: CoroutineScope,
) : PlaybackController {
    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(playerListener)
            pushState()
            startPositionLoop()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = pushState()
    }

    /** Position advances continuously but fires no events — poll it for a smooth readout. */
    private fun startPositionLoop() {
        scope.launch {
            while (isActive) {
                controller?.let { c ->
                    if (c.isPlaying) {
                        _state.value = _state.value.copy(
                            positionMs = c.currentPosition.coerceAtLeast(0),
                            durationMs = c.duration.coerceAtLeast(0),
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun pushState() {
        val c = controller
        if (c == null) {
            _state.value = PlayerUiState(isConnected = false)
            return
        }
        val md = c.mediaMetadata
        _state.value = PlayerUiState(
            isConnected = true,
            isPlaying = c.isPlaying,
            currentMediaId = c.currentMediaItem?.mediaId,
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            artworkUri = md.artworkUri?.toString(),
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            repeatMode = RepeatModeMapper.toApp(c.repeatMode),
            shuffleEnabled = c.shuffleModeEnabled,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
            queue = c.snapshotQueue(),
            currentIndex = c.currentMediaItemIndex,
        )
    }

    // --- Controls used by the UI ---

    override fun setQueueAndPlay(tracks: List<PlayableTrack>, startIndex: Int) {
        val c = controller ?: return
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    override fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() { controller?.seekToNext() }
    override fun previous() { controller?.seekToPrevious() }
    override fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    override fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    override fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = RepeatModeMapper.toPlayer(RepeatModeMapper.next(RepeatModeMapper.toApp(c.repeatMode)))
    }
}

private fun MediaController.snapshotQueue(): List<QueueItem> =
    (0 until mediaItemCount).map { i ->
        val item = getMediaItemAt(i)
        QueueItem(
            mediaId = item.mediaId,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            artist = item.mediaMetadata.artist?.toString().orEmpty(),
        )
    }

private fun PlayableTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .apply { artworkUri?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                .build(),
        )
        .build()
