package com.youtubestream.app.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeMapperTest {

    @Test
    fun toApp_mapsPlayerConstants() {
        assertEquals(AppRepeatMode.OFF, RepeatModeMapper.toApp(Player.REPEAT_MODE_OFF))
        assertEquals(AppRepeatMode.TRACK, RepeatModeMapper.toApp(Player.REPEAT_MODE_ONE))
        assertEquals(AppRepeatMode.QUEUE, RepeatModeMapper.toApp(Player.REPEAT_MODE_ALL))
    }

    @Test
    fun toPlayer_mapsAppModes() {
        assertEquals(Player.REPEAT_MODE_OFF, RepeatModeMapper.toPlayer(AppRepeatMode.OFF))
        assertEquals(Player.REPEAT_MODE_ONE, RepeatModeMapper.toPlayer(AppRepeatMode.TRACK))
        assertEquals(Player.REPEAT_MODE_ALL, RepeatModeMapper.toPlayer(AppRepeatMode.QUEUE))
    }

    @Test
    fun next_cyclesOffTrackQueueOff() {
        assertEquals(AppRepeatMode.TRACK, RepeatModeMapper.next(AppRepeatMode.OFF))
        assertEquals(AppRepeatMode.QUEUE, RepeatModeMapper.next(AppRepeatMode.TRACK))
        assertEquals(AppRepeatMode.OFF, RepeatModeMapper.next(AppRepeatMode.QUEUE))
    }
}
