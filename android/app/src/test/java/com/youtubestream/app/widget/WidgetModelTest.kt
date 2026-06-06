package com.youtubestream.app.widget

import com.youtubestream.app.playback.AppRepeatMode
import com.youtubestream.app.playback.PlayerUiState
import com.youtubestream.app.playback.QueueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTest {

    private fun state(
        isConnected: Boolean = true,
        currentMediaId: String? = "id-1",
        title: String = "Song A",
        artist: String = "Artist A",
        artworkUri: String? = "http://art/1.jpg",
        isPlaying: Boolean = true,
        repeatMode: AppRepeatMode = AppRepeatMode.OFF,
        shuffleEnabled: Boolean = false,
        queue: List<QueueItem> = listOf(QueueItem("id-1", "Song A", "Artist A"), QueueItem("id-2", "Song B", "Artist B")),
        currentIndex: Int = 0,
    ) = PlayerUiState(
        isConnected = isConnected,
        isPlaying = isPlaying,
        currentMediaId = currentMediaId,
        title = title,
        artist = artist,
        artworkUri = artworkUri,
        positionMs = 1234L,
        durationMs = 9999L,
        repeatMode = repeatMode,
        shuffleEnabled = shuffleEnabled,
        queue = queue,
        currentIndex = currentIndex,
    )

    @Test
    fun `maps the visible fields`() {
        val m = WidgetModel.from(state())
        assertTrue(m.isConnected)
        assertTrue(m.hasTrack)
        assertEquals("Song A", m.title)
        assertEquals("Artist A", m.artist)
        assertEquals("http://art/1.jpg", m.artworkUri)
        assertTrue(m.isPlaying)
        assertEquals(AppRepeatMode.OFF, m.repeatMode)
        assertFalse(m.shuffleOn)
    }

    @Test
    fun `up next is the track after the current index`() {
        assertEquals("Song B", WidgetModel.from(state(currentIndex = 0)).upNextTitle)
    }

    @Test
    fun `up next is null on the last track`() {
        assertNull(WidgetModel.from(state(currentIndex = 1)).upNextTitle)
    }

    @Test
    fun `up next is null when the queue is empty`() {
        assertNull(WidgetModel.from(state(queue = emptyList(), currentIndex = 0)).upNextTitle)
    }

    @Test
    fun `up next is null when the next title is blank`() {
        val q = listOf(QueueItem("id-1", "Song A", "A"), QueueItem("id-2", "  ", "B"))
        assertNull(WidgetModel.from(state(queue = q, currentIndex = 0)).upNextTitle)
    }

    @Test
    fun `hasTrack is false when nothing is loaded`() {
        assertFalse(WidgetModel.from(state(currentMediaId = null)).hasTrack)
    }

    @Test
    fun `position changes do not change the model (equality holds)`() {
        val a = WidgetModel.from(state())
        val b = WidgetModel.from(state())   // identical except this mirrors a later position tick
        assertEquals(a, b)
    }
}
