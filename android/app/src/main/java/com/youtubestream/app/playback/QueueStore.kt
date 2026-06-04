package com.youtubestream.app.playback

/**
 * Persists the playback queue across process death. A port (interface) so [PlaybackConnection] stays
 * free of Android storage APIs and is fakeable in tests; the real adapter is DataStore-backed.
 */
interface QueueStore {
    /** The last saved queue, or null if nothing was ever saved / the payload was unreadable. */
    suspend fun load(): PersistedQueue?

    suspend fun save(queue: PersistedQueue)
}
