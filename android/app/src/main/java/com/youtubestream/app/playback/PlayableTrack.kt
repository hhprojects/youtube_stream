package com.youtubestream.app.playback

import kotlinx.serialization.Serializable

/**
 * One simple track description the UI hands to the player. It is also the unit we persist across
 * sessions, hence [Serializable]: it carries the URI, which the MediaController strips when reading
 * the queue back across the session boundary — so this is the only reliable record of it.
 */
@Serializable
data class PlayableTrack(
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
)
