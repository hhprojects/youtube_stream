package com.youtubestream.app.playback

import com.youtubestream.app.data.repository.PodcastSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Persists per-episode resume position. App-scoped, started once. Keeps podcast logic out of
 * PlaybackConnection (which stays content-agnostic) and Media3 out of the repository.
 */
class PodcastProgressWriter(
    private val connection: PlaybackConnection,
    private val repo: PodcastSource,
    private val scope: CoroutineScope,
    private val finishFraction: Float = 0.95f,
) {
    private var currentMediaId: String? = null
    private var currentIsEpisode = false
    private var lastWrittenMs = 0L

    fun start() {
        // Re-classify only when the playing item changes (one DB lookup per track change).
        connection.state.map { it.currentMediaId }.distinctUntilChanged()
            .onEach { id ->
                currentMediaId = id
                currentIsEpisode = id != null && repo.isEpisode(id)
                lastWrittenMs = 0L
            }
            .launchIn(scope)

        // Persist position while an episode plays; throttle to ~once / 5s of movement.
        connection.state
            .onEach { st ->
                val id = st.currentMediaId ?: return@onEach
                if (!currentIsEpisode || id != currentMediaId) return@onEach
                val pos = st.positionMs
                val dur = st.durationMs
                if (dur > 0 && pos >= dur * finishFraction) {
                    scope.launch { repo.markFinished(id) }
                    return@onEach
                }
                if (pos - lastWrittenMs >= 5_000 || (pos in 1 until 5_000 && lastWrittenMs == 0L)) {
                    lastWrittenMs = pos
                    scope.launch { repo.updateResumePosition(id, pos, System.currentTimeMillis()) }
                }
            }
            .launchIn(scope)
    }
}
