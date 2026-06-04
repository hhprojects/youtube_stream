package com.youtubestream.app.playback

import kotlinx.serialization.json.Json

/**
 * Pure JSON (de)serialization for [PersistedQueue]. No Android dependencies, so it unit-tests on the
 * JVM. [decode] never throws: a malformed or legacy payload is reported as null ("nothing saved").
 */
object QueueSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(queue: PersistedQueue): String =
        json.encodeToString(PersistedQueue.serializer(), queue)

    fun decode(raw: String): PersistedQueue? =
        try {
            json.decodeFromString(PersistedQueue.serializer(), raw)
        } catch (e: Exception) {
            null
        }
}
