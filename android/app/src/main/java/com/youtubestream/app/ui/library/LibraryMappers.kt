package com.youtubestream.app.ui.library

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.playback.PlayableTrack
import java.io.File

/**
 * A downloaded library row → a track the player streams from local disk.
 * `File.toURI()` yields a percent-encoded `file:` URI, so spaces / `#` / `?` in yt-dlp filenames
 * survive; on device, ExoPlayer's FileDataSource serves the `file` scheme.
 */
fun LibrarySong.toPlayableTrack(): PlayableTrack = PlayableTrack(
    mediaId = id,
    uri = File(localPath).toURI().toString(),
    title = title,
    artist = artist,
)
