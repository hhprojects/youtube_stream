package com.youtubestream.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore instance per process, named "settings". Must be a top-level delegate.
private val Context.dataStore by preferencesDataStore(name = "settings")

/** Supplies scheme/host/port for the Pi; the `/api/...` path lives in the Retrofit endpoints. */
const val DEFAULT_SERVER_URL = "http://<PI_IP>:3001"

class SettingsDataStore(private val context: Context) {

    private val serverUrlKey = stringPreferencesKey("server_url")
    private val authTokenKey = stringPreferencesKey("auth_token") // reserved, unused (YAGNI)

    /** Reactive: current value now, plus a new emission on every change. */
    val serverUrl: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[serverUrlKey] ?: DEFAULT_SERVER_URL }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[serverUrlKey] = url }
    }
}
