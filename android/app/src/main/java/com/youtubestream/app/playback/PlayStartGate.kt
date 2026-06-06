package com.youtubestream.app.playback

/**
 * Pure decision logic for "a track just started playing" — feed it player signals, it returns the
 * songId to count once per start, or null. No Android imports → JVM-tested.
 *
 * Counts when the player is playing and either the current item just changed (transition) or
 * playback just began. A `counted` flag prevents double-counting one start across the two signals;
 * a transition resets it, so repeat-one loops and replays of the same id count again.
 */
class PlayStartGate {
    private var currentId: String? = null
    private var counted = false

    /** Call on EVENT_MEDIA_ITEM_TRANSITION (item became current, possibly same id via REPEAT). */
    fun onTransition(newId: String?, isPlaying: Boolean): String? {
        currentId = newId
        counted = false
        return maybeCount(isPlaying)
    }

    /** Call on EVENT_IS_PLAYING_CHANGED (or any event) to re-evaluate. */
    fun onPlayingChanged(isPlaying: Boolean, currentMediaId: String?): String? {
        if (currentMediaId != currentId) {   // a start with no preceding transition (e.g. first queue set)
            currentId = currentMediaId
            counted = false
        }
        return maybeCount(isPlaying)
    }

    private fun maybeCount(isPlaying: Boolean): String? {
        val id = currentId
        if (isPlaying && !counted && id != null) {
            counted = true
            return id
        }
        return null
    }
}
