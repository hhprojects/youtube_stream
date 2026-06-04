package com.youtubestream.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for the queue (de)serialization that backs cross-session persistence.
 * No Android deps — this is the fast base of the testing pyramid.
 */
class QueueSerializerTest {

    private val sample = PersistedQueue(
        tracks = listOf(
            PlayableTrack("id1", "file:///songs/a.mp3", "Song A", "Artist A"),
            PlayableTrack("id2", "file:///songs/b.mp3", "Song B", "Artist B", artworkUri = "file:///art/b.png"),
        ),
        currentIndex = 1,
        positionMs = 42_000L,
    )

    @Test
    fun `round-trips a queue without loss`() {
        val decoded = QueueSerializer.decode(QueueSerializer.encode(sample))
        assertEquals(sample, decoded)
    }

    @Test
    fun `decode returns null on malformed json`() {
        assertNull(QueueSerializer.decode("not json {{{"))
    }

    @Test
    fun `decode returns null on empty input`() {
        assertNull(QueueSerializer.decode(""))
    }

    @Test
    fun `decode tolerates unknown keys for forward-compat`() {
        val withExtra = """{"tracks":[],"currentIndex":0,"positionMs":0,"futureField":true}"""
        assertEquals(PersistedQueue(), QueueSerializer.decode(withExtra))
    }

    @Test
    fun `decode applies defaults for missing fields`() {
        val minimal = """{"tracks":[{"mediaId":"x","uri":"u","title":"t","artist":"a"}]}"""
        val decoded = QueueSerializer.decode(minimal)!!
        assertEquals(1, decoded.tracks.size)
        assertEquals(0, decoded.currentIndex)
        assertEquals(0L, decoded.positionMs)
        assertNull(decoded.tracks[0].artworkUri)
    }
}
