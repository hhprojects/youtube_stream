package com.youtubestream.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayStartGateTest {

    @Test
    fun freshPlay_countsFirstTrack_evenWhenPlayIsAsync() {
        val gate = PlayStartGate()
        // setMediaItems fires a transition while not yet playing (play() is async):
        assertNull(gate.onTransition(newId = "A", isPlaying = false))
        // play() lands → isPlaying true:
        assertEquals("A", gate.onPlayingChanged(isPlaying = true, currentMediaId = "A"))
    }

    @Test
    fun autoAdvance_countsNextTrack_withNoIsPlayingEdge() {
        val gate = PlayStartGate()
        assertEquals("A", gate.onPlayingChanged(true, "A"))
        // A ends, B becomes current while still playing — transition only, no isPlaying change:
        assertEquals("B", gate.onTransition(newId = "B", isPlaying = true))
    }

    @Test
    fun pauseThenResumeSameTrack_doesNotDoubleCount() {
        val gate = PlayStartGate()
        assertEquals("A", gate.onPlayingChanged(true, "A"))
        assertNull(gate.onPlayingChanged(false, "A"))   // pause
        assertNull(gate.onPlayingChanged(true, "A"))    // resume — same start, no recount
    }

    @Test
    fun coldStartRestore_whilePaused_producesNoPhantomPlay() {
        val gate = PlayStartGate()
        assertNull(gate.onTransition(newId = "A", isPlaying = false))
        // ...and nothing ever starts playing → never counted.
    }

    @Test
    fun repeatOneLoop_countsEachLoop() {
        val gate = PlayStartGate()
        assertEquals("A", gate.onPlayingChanged(true, "A"))
        // REPEAT transition to the same id while playing → counts again:
        assertEquals("A", gate.onTransition(newId = "A", isPlaying = true))
    }

    @Test
    fun isPlayingChangedSyncsCurrentIdWhenNoTransitionFired() {
        val gate = PlayStartGate()
        // No transition first (e.g. id known only at play): still counts.
        assertEquals("A", gate.onPlayingChanged(true, "A"))
    }

    @Test
    fun nullCurrentId_neverCounts() {
        val gate = PlayStartGate()
        assertNull(gate.onPlayingChanged(true, null))
        assertNull(gate.onTransition(null, true))
    }
}
