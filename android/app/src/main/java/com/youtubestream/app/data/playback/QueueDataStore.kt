package com.youtubestream.app.data.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.youtubestream.app.playback.PersistedQueue
import com.youtubestream.app.playback.QueueSerializer
import com.youtubestream.app.playback.QueueStore
import kotlinx.coroutines.flow.first

// One DataStore per process, named "playback_queue" (a separate file from "settings"). Top-level delegate.
private val Context.queueDataStore by preferencesDataStore(name = "playback_queue")

/** DataStore-backed [QueueStore]: the whole queue lives as one JSON string under a single key. */
class QueueDataStore(private val context: Context) : QueueStore {

    private val queueKey = stringPreferencesKey("queue_json")

    override suspend fun load(): PersistedQueue? {
        val raw = context.queueDataStore.data.first()[queueKey] ?: return null
        return QueueSerializer.decode(raw)
    }

    override suspend fun save(queue: PersistedQueue) {
        context.queueDataStore.edit { prefs -> prefs[queueKey] = QueueSerializer.encode(queue) }
    }
}
