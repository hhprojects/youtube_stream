package com.youtubestream.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** One-shot: emits the mediaId of a track that failed because its local file is gone (→ prune the row). */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** One-shot human-readable playback errors for the UI to surface (toast). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

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

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
                val idx = c.currentMediaItemIndex
                val failedId = c.currentMediaItem?.mediaId
                // Drop the dead item (not just skip) — the player advances to the next, and if several
                // files are missing each error removes one and converges. seekToNext would loop forever
                // on a missing LAST track (no-op skip → re-prepare same dead file → error again).
                if (idx in 0 until c.mediaItemCount) c.removeMediaItem(idx)
                c.prepare()
                c.play()
                failedId?.let { _errors.tryEmit(it) }            // → AppContainer prunes the library row
                _messages.tryEmit("Skipped a track — its file was missing (removed from library)")
                return
            }
            // Any other failure (network, decode, unsupported source): surface it and keep the queue
            // moving, but only skip when there's a next item — re-preparing a failing LAST track loops.
            _messages.tryEmit("Can't play this track (${error.errorCodeName})")
            if (c.hasNextMediaItem()) {
                c.seekToNext()
                c.prepare()
                c.play()
            }
        }
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
