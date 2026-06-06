package com.youtubestream.app.widget

import com.youtubestream.app.playback.PlaybackController
import com.youtubestream.app.playback.PlayableTrack
import com.youtubestream.app.playback.PlayerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Records which control method each widget action invokes. */
private class RecordingController : PlaybackController {
    override val state: StateFlow<PlayerUiState> = MutableStateFlow(PlayerUiState())
    val calls = mutableListOf<String>()
    override fun setQueueAndPlay(tracks: List<PlayableTrack>, startIndex: Int) { calls += "setQueueAndPlay" }
    override fun togglePlayPause() { calls += "togglePlayPause" }
    override fun next() { calls += "next" }
    override fun previous() { calls += "previous" }
    override fun seekTo(positionMs: Long) { calls += "seekTo" }
    override fun toggleShuffle() { calls += "toggleShuffle" }
    override fun cycleRepeat() { calls += "cycleRepeat" }
}

class WidgetIntentsTest {

    @Test
    fun `each action maps to its control method`() {
        val cases = mapOf(
            WidgetIntents.ACTION_TOGGLE to "togglePlayPause",
            WidgetIntents.ACTION_PREV to "previous",
            WidgetIntents.ACTION_NEXT to "next",
            WidgetIntents.ACTION_SHUFFLE to "toggleShuffle",
            WidgetIntents.ACTION_REPEAT to "cycleRepeat",
        )
        for ((action, expected) in cases) {
            val c = RecordingController()
            c.dispatchWidgetAction(action)
            assertEquals(listOf(expected), c.calls)
        }
    }

    @Test
    fun `unknown action does nothing`() {
        val c = RecordingController()
        c.dispatchWidgetAction("nope")
        assertEquals(emptyList<String>(), c.calls)
    }
}
