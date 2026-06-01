package com.youtubestream.app.ui.player

import com.youtubestream.app.playback.PlayableTrack

/**
 * TEMPORARY dev convenience: public-domain HTTPS audio so the Player works without the Pi/yt-dlp.
 * Surfaced by the Player's empty state. Remove before parity/cutover.
 */
object DebugTracks {
    val TEST_TRACKS = listOf(
        PlayableTrack(
            mediaId = "t1",
            uri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            title = "Test Track 1",
            artist = "Sevish",
        ),
        PlayableTrack(
            mediaId = "t2",
            uri = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
            title = "Test Track 2",
            artist = "Epoq",
        ),
    )
}
