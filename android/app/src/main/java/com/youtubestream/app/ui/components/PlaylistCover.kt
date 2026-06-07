package com.youtubestream.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A playlist's cover: the custom cover URL if set, else the first member song's art, else the
 * music-note placeholder (which [SongArtwork] draws for a null url). The fallback chain is a
 * single `?:` — no behavior worth unit-testing, just a thin wrapper for reuse.
 */
@Composable
fun PlaylistCover(
    coverArtUrl: String?,
    firstSongArtworkUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    SongArtwork(url = coverArtUrl ?: firstSongArtworkUrl, modifier = modifier, size = size)
}
