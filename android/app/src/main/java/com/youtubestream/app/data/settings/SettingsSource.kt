package com.youtubestream.app.data.settings

import kotlinx.coroutines.flow.Flow

/** The settings operations a ViewModel needs — fakeable in JVM tests. */
interface SettingsSource {
    val serverUrl: Flow<String>
    suspend fun setServerUrl(url: String)
}
