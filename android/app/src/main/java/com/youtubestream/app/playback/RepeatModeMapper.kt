package com.youtubestream.app.playback

import androidx.media3.common.Player

object RepeatModeMapper {
    fun toApp(playerRepeatMode: Int): AppRepeatMode = when (playerRepeatMode) {
        Player.REPEAT_MODE_ONE -> AppRepeatMode.TRACK
        Player.REPEAT_MODE_ALL -> AppRepeatMode.QUEUE
        else -> AppRepeatMode.OFF
    }

    fun toPlayer(mode: AppRepeatMode): Int = when (mode) {
        AppRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        AppRepeatMode.TRACK -> Player.REPEAT_MODE_ONE
        AppRepeatMode.QUEUE -> Player.REPEAT_MODE_ALL
    }

    /** Cycle order matches the RN app: OFF -> TRACK -> QUEUE -> OFF. */
    fun next(mode: AppRepeatMode): AppRepeatMode = when (mode) {
        AppRepeatMode.OFF -> AppRepeatMode.TRACK
        AppRepeatMode.TRACK -> AppRepeatMode.QUEUE
        AppRepeatMode.QUEUE -> AppRepeatMode.OFF
    }
}
