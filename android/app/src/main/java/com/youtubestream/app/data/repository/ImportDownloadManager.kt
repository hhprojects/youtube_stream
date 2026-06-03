package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.PiSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-song bulk-import status. `Completed` isn't here — a finished id leaves the importable list. */
sealed interface ImportItemState {
    data class Downloading(val fraction: Float) : ImportItemState
    data class Failed(val message: String) : ImportItemState
}

/**
 * App-scoped orchestrator for bulk Pi imports. Held by [com.youtubestream.app.di.AppContainer] — NOT a
 * ViewModel — so an in-flight import survives the user leaving the Import screen: the download loop runs
 * on an application-scoped [scope], and progress lives in a [downloads] StateFlow the screen re-attaches
 * to. (When this lived in the ViewModel, backing out of Import cancelled `viewModelScope` mid-stream.)
 */
class ImportDownloadManager(
    private val importer: Importer,
    private val scope: CoroutineScope,
) {
    private val _downloads = MutableStateFlow<Map<String, ImportItemState>>(emptyMap())
    val downloads: StateFlow<Map<String, ImportItemState>> = _downloads.asStateFlow()

    /** Streams each song sequentially, updating [downloads]. Ids already in flight are skipped. */
    fun enqueue(songs: List<PiSong>) {
        // Exclude ids already importing — re-tapping Download mid-import must not start a second
        // stream writing the same target file.
        val toGet = songs.filter { _downloads.value[it.id] !is ImportItemState.Downloading }
        if (toGet.isEmpty()) return
        // Mark each "downloading" immediately so rows react before the first byte arrives.
        _downloads.update { current -> current + toGet.associate { it.id to ImportItemState.Downloading(0f) } }
        scope.launch {
            for (song in toGet) {
                importer.importSong(song).collect { st ->
                    _downloads.update { current ->
                        when (st) {
                            is DownloadState.InProgress ->
                                current + (song.id to ImportItemState.Downloading(st.fraction))
                            is DownloadState.Completed ->
                                current - song.id        // inserted → drops off importable via the Room flow
                            is DownloadState.Failed ->
                                current + (song.id to ImportItemState.Failed(st.error.message ?: "Download failed"))
                        }
                    }
                }
            }
        }
    }
}
