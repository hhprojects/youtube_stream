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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** How long to coalesce a burst of player events before writing the queue to disk. */
private const val SAVE_DEBOUNCE_MS = 400L

/**
 * The UI's only door to playback. Connects a [MediaController] to [PlaybackService],
 * mirrors player events into [state], and exposes control methods. Main-thread-confined.
 */
class PlaybackConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val queueStore: QueueStore,
) : PlaybackController {
    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** One-shot: emits the mediaId of a track that failed because its local file is gone (→ prune the row). */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** One-shot human-readable playback errors for the UI to surface (toast). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // Guaranteed-delivery (buffered) — a recorded play must not be dropped, unlike best-effort errors.
    private val _playStarts = Channel<String>(capacity = Channel.UNLIMITED)
    /** Emits the songId (== LibrarySong.id) each time a track actually starts playing. */
    val playStarts: Flow<String> = _playStarts.receiveAsFlow()

    private val playStartGate = PlayStartGate()

    private var controller: MediaController? = null

    /**
     * Our mirror of the controller timeline — track + manual flag per entry, in timeline order.
     * Source of truth for URIs (the controller strips them across the session boundary) and for
     * the two-tier queue flags. Persisted via QueueStore.
     */
    private var entries: List<QueueEntry> = emptyList()

    /**
     * App-managed shuffle. Media3's shuffleModeEnabled is NEVER used: its hidden permutation would
     * bypass the manual block. Instead toggling shuffle permutes the context tail in the timeline
     * itself, so the timeline (and the Up Next list) always IS the true play order.
     */
    private var shuffleOn = false
    private var originalOrder: List<String> = emptyList()
    private val random: Random = Random.Default

    /** Debounces queue/position writes so a burst of player events collapses into one save. */
    private var saveJob: Job? = null

    /** Sleep timer: a pending timed-pause job, the wall-clock it fires at, and the end-of-track flag. */
    private var sleepJob: Job? = null
    private var sleepEndsAtMs: Long? = null
    private var sleepAtTrackEnd = false

    /** One-slot deferred command (a widget tap that arrived before connect finished) + its callback. */
    private var pendingAction: (PlaybackController.() -> Unit)? = null
    private var pendingOnApplied: (() -> Unit)? = null

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
            restoreQueueThenFlush()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // End-of-track sleep: pause when a track finishes on its own (auto-advance), not on a manual skip.
            if (sleepAtTrackEnd && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                sleepAtTrackEnd = false
                controller?.pause()   // onEvents → pushState clears the flag in the UI
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            pushState()
            // Record a real track-start into play-history (For You). The pure PlayStartGate decides
            // when one play counts; gating on these two events avoids phantom (paused restore) and
            // missed (async play / auto-advance) counts. trySend never drops on an UNLIMITED channel.
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                playStartGate.onTransition(player.currentMediaItem?.mediaId, player.isPlaying)
                    ?.let { _playStarts.trySend(it) }
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                playStartGate.onPlayingChanged(player.isPlaying, player.currentMediaItem?.mediaId)
                    ?.let { _playStarts.trySend(it) }
            }
            // Persist when the queue, the current track, the play state, the position, or the
            // shuffle/repeat modes change — not on the smooth position tick (that fires no event).
            // Debounced to one write.
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                )
            ) {
                scheduleSave()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
                val idx = c.currentMediaItemIndex
                val failedId = c.currentMediaItem?.mediaId
                // Drop the dead item (not just skip) — the player advances to the next, and if several
                // files are missing each error removes one and converges. seekToNext would loop forever
                // on a missing LAST track (no-op skip → re-prepare same dead file → error again).
                if (idx in 0 until c.mediaItemCount) {
                    c.removeMediaItem(idx)
                    dropEntry(idx)   // keep our URI list aligned with the controller's timeline
                }
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
            shuffleEnabled = shuffleOn,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
            queue = c.snapshotQueue(entries),
            currentIndex = c.currentMediaItemIndex,
            playbackSpeed = c.playbackParameters.speed,
            isPodcast = entries.getOrNull(c.currentMediaItemIndex)?.track?.isPodcast == true,
            sleepTimerEndsAtMs = sleepEndsAtMs,
            sleepAtTrackEnd = sleepAtTrackEnd,
        )
    }

    // --- Controls used by the UI ---

    override fun setQueueAndPlay(tracks: List<PlayableTrack>, startIndex: Int, startPositionMs: Long, shuffled: Boolean) {
        val c = controller ?: return
        val plan = TwoTierQueue.buildSetQueue(entries, c.currentMediaItemIndex, tracks, startIndex, shuffled, random)
        entries = plan.entries
        shuffleOn = shuffled
        originalOrder = plan.originalOrder
        // Media3 applies the start position atomically at prepare — no post-prepare seek race.
        c.setMediaItems(plan.entries.map { it.track.toMediaItem() }, plan.startIndex, startPositionMs)
        c.prepare()
        c.play()
        // A song queue plays at 1×; a podcast's chosen speed persists across episodes (don't reset it).
        if (plan.entries.none { it.track.isPodcast }) c.setPlaybackSpeed(1f)
        saveNow()   // the high-value "new queue" event — persist now, don't risk the debounce window
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
        if (entries.isEmpty()) return
        if (!shuffleOn) {
            val plan = TwoTierQueue.shuffleTail(entries, c.currentMediaItemIndex, random)
            originalOrder = entries.drop(plan.tailStart).map { it.track.mediaId }
            applyTail(c, plan)
            shuffleOn = true
        } else {
            val plan = TwoTierQueue.unshuffleTail(entries, c.currentMediaItemIndex, originalOrder)
            applyTail(c, plan)
            shuffleOn = false
            originalOrder = emptyList()
        }
        pushState()
        saveNow()
    }

    /** Replaces the upcoming-context tail (everything after the manual block) in one remove+add. */
    private fun applyTail(c: MediaController, plan: TwoTierQueue.TailPlan) {
        if (plan.tailStart >= c.mediaItemCount) return   // nothing after the block
        c.removeMediaItems(plan.tailStart, c.mediaItemCount)
        c.addMediaItems(plan.newTail.map { it.track.toMediaItem() })
        entries = entries.take(plan.tailStart) + plan.newTail
    }

    override fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = RepeatModeMapper.toPlayer(RepeatModeMapper.next(RepeatModeMapper.toApp(c.repeatMode)))
    }

    override fun setSpeed(speed: Float) { controller?.setPlaybackSpeed(speed) }

    override fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        val target = (c.currentPosition + deltaMs).coerceIn(0L, if (c.duration > 0) c.duration else Long.MAX_VALUE)
        c.seekTo(target)
    }

    // --- Queue insertion. Append / play-next; if the queue is empty, start a fresh queue + play so the
    // action is never a silent no-op. Keep `entries` aligned; saveNow() persists the high-value edit. ---

    override fun addToQueue(tracks: List<PlayableTrack>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        if (c.mediaItemCount == 0) { setQueueAndPlay(tracks); return }
        val at = TwoTierQueue.addToQueueIndex(entries, c.currentMediaItemIndex).coerceAtMost(c.mediaItemCount)
        c.addMediaItems(at, tracks.map { it.toMediaItem() })
        entries = entries.toMutableList().apply { addAll(at, tracks.map { QueueEntry(it, isManual = true) }) }
        saveNow()
    }

    override fun playNext(tracks: List<PlayableTrack>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        if (c.mediaItemCount == 0) { setQueueAndPlay(tracks); return }
        val at = TwoTierQueue.playNextIndex(c.currentMediaItemIndex).coerceIn(0, c.mediaItemCount)
        c.addMediaItems(at, tracks.map { it.toMediaItem() })
        entries = entries.toMutableList().apply { addAll(at, tracks.map { QueueEntry(it, isManual = true) }) }
        saveNow()
    }

    // --- Queue editing. Each keeps `entries` aligned with the controller's timeline; the
    // resulting timeline/transition events drive pushState + a debounced save automatically. ---

    override fun playQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        val plan = TwoTierQueue.jumpPlan(entries, c.currentMediaItemIndex, index)
        if (plan.moveCount > 0) {
            // Pull the manual block along so queued songs still play right after the tapped item.
            c.moveMediaItems(plan.moveFrom, plan.moveFrom + plan.moveCount, plan.moveNewIndex)
            val m = entries.toMutableList()
            val block = List(plan.moveCount) { m.removeAt(plan.moveFrom) }
            m.addAll(plan.moveNewIndex, block)
            entries = m
        }
        c.seekToDefaultPosition(plan.seekIndex)
        c.play()
    }

    override fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        val count = c.mediaItemCount
        if (from in 0 until count && to in 0 until count && from != to) {
            entries = TwoTierQueue.move(entries, c.currentMediaItemIndex, from, to)
            c.moveMediaItem(from, to)
        }
    }

    override fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
            dropEntry(index)
        }
    }

    override fun clearUpNext() {
        val c = controller ?: return
        val from = c.currentMediaItemIndex + 1 + TwoTierQueue.pendingCount(entries, c.currentMediaItemIndex)
        if (from in 1 until c.mediaItemCount) {
            c.removeMediaItems(from, c.mediaItemCount)
            entries = entries.take(from)
        }
    }

    override fun clearManualQueue() {
        val c = controller ?: return
        val cur = c.currentMediaItemIndex
        val count = TwoTierQueue.pendingCount(entries, cur)
        if (count > 0) {
            c.removeMediaItems(cur + 1, cur + 1 + count)
            entries = entries.take(cur + 1) + entries.drop(cur + 1 + count)
        }
    }

    // --- Sleep timer ---

    override fun setSleepTimer(durationMs: Long) {
        sleepJob?.cancel()
        sleepAtTrackEnd = false
        sleepEndsAtMs = System.currentTimeMillis() + durationMs
        sleepJob = scope.launch {
            delay(durationMs)
            controller?.pause()
            sleepEndsAtMs = null
            sleepJob = null
            pushState()
        }
        pushState()
    }

    override fun setSleepTimerEndOfTrack() {
        sleepJob?.cancel()
        sleepJob = null
        sleepEndsAtMs = null
        sleepAtTrackEnd = true
        pushState()
    }

    override fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepEndsAtMs = null
        sleepAtTrackEnd = false
        pushState()
    }

    override fun stop() {
        val c = controller ?: return
        c.stop()                                  // halt playback
        c.clearMediaItems()                       // empty the timeline → currentMediaItem becomes null
        entries = emptyList()
        shuffleOn = false
        originalOrder = emptyList()
        sleepJob?.cancel(); sleepJob = null; sleepEndsAtMs = null; sleepAtTrackEnd = false
        saveJob?.cancel()                         // cancel any debounced save the clear events would trigger
        scope.launch { queueStore.clear() }       // forget the persisted session on disk
        pushState()                               // emit the idle state now (don't wait for an event)
    }

    /**
     * Run [action] now if the controller is connected, else stash it (one slot) and run it once
     * [connect] has wired the controller and restored the queue. [onApplied] fires right after the
     * action runs — the widget uses it to release its goAsync receiver once the cold-start tap took
     * effect. Last write wins if two taps arrive while still disconnected.
     */
    fun runWhenReady(onApplied: (() -> Unit)? = null, action: PlaybackController.() -> Unit) {
        if (controller != null) {
            action(this)
            onApplied?.invoke()
        } else {
            pendingAction = action
            pendingOnApplied = onApplied
        }
    }

    // --- Cross-session persistence ---

    /** Persist now, capturing the snapshot at call time so a later change can't alter what we write. */
    private fun saveNow() {
        val snapshot = currentSnapshot() ?: return
        scope.launch { queueStore.save(snapshot) }
    }

    /** Coalesce a burst of player events into one delayed write of the latest snapshot. */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            currentSnapshot()?.let { queueStore.save(it) }
        }
    }

    /** The queue + current position to persist, or null when there's nothing worth saving. */
    private fun currentSnapshot(): PersistedQueue? {
        val c = controller ?: return null
        if (entries.isEmpty()) return null
        return PersistedQueue(
            tracks = entries.map { it.track },
            currentIndex = c.currentMediaItemIndex.coerceIn(0, entries.lastIndex),
            positionMs = c.currentPosition.coerceAtLeast(0L),
            shuffleEnabled = shuffleOn,
            repeatMode = RepeatModeMapper.toApp(c.repeatMode),
            manualFlags = entries.map { it.isManual },
            originalOrder = originalOrder,
        )
    }

    private fun dropEntry(index: Int) {
        if (index in entries.indices) {
            entries = entries.toMutableList().apply { removeAt(index) }
        }
    }

    /**
     * On connect, if the service has no live queue (the process had been killed), rehydrate the last
     * session's queue — seeked to where the user left off, but paused. Never auto-plays. Then flush any
     * pending widget command. flushPending runs on every path (incl. no-saved-queue) so a cold-start
     * widget tap is never stranded.
     */
    private fun restoreQueueThenFlush() {
        val c = controller ?: return
        if (c.mediaItemCount > 0) {                       // service survived with a live queue — leave it
            flushPending()
            return
        }
        scope.launch {
            try {
                val saved = queueStore.load()
                // mediaItemCount == 0 guard: a song may have been picked while load() suspended — don't clobber.
                if (saved != null && saved.tracks.isNotEmpty() && c.mediaItemCount == 0) {
                    entries = saved.tracks.mapIndexed { i, t ->
                        QueueEntry(t, isManual = saved.manualFlags.getOrNull(i) == true)
                    }
                    shuffleOn = saved.shuffleEnabled
                    originalOrder = saved.originalOrder
                    // Repeat is re-applied on the controller (a fresh ExoPlayer defaults it off).
                    // Shuffle is app-managed now: the persisted timeline already IS the play order,
                    // so the controller's shuffleModeEnabled is never touched.
                    c.repeatMode = RepeatModeMapper.toPlayer(saved.repeatMode)
                    val startIndex = saved.currentIndex.coerceIn(0, saved.tracks.lastIndex)
                    c.setMediaItems(
                        saved.tracks.map { it.toMediaItem() },
                        startIndex,
                        saved.positionMs.coerceAtLeast(0L),
                    )
                    c.prepare()                           // buffer & show it, but stay paused (no play())
                    pushState()
                }
            } finally {
                flushPending()
            }
        }
    }

    /** Run and clear the one-slot deferred command, then signal completion. */
    private fun flushPending() {
        val action = pendingAction ?: return
        pendingAction = null
        val onApplied = pendingOnApplied
        pendingOnApplied = null
        action(this)
        onApplied?.invoke()
    }
}

private fun MediaController.snapshotQueue(entries: List<QueueEntry>): List<QueueItem> =
    (0 until mediaItemCount).map { i ->
        val item = getMediaItemAt(i)
        QueueItem(
            mediaId = item.mediaId,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            artist = item.mediaMetadata.artist?.toString().orEmpty(),
            isManual = entries.getOrNull(i)?.isManual == true,   // defensive zip; sizes match by construction
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
