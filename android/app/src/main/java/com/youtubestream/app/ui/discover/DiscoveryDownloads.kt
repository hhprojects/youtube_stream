package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.repository.Downloader
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared tap->download->play for discovered songs. Holds a per-videoId progress map the cards render,
 * and plays the song once its two-step download completes. [scope] is the caller's viewModelScope.
 */
class DiscoveryDownloads(
    private val downloader: Downloader,
    private val play: (LibrarySong) -> Unit,
) {
    private val _downloads = MutableStateFlow<Map<String, ItemDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, ItemDownload>> = _downloads.asStateFlow()

    fun download(scope: CoroutineScope, videoId: String, title: String, artworkUrl: String?) {
        if (_downloads.value[videoId] is ItemDownload.Downloading) return   // ignore double-taps
        _downloads.update { it + (videoId to ItemDownload.Downloading(0f)) }
        scope.launch {
            downloader.download(videoId, title, artworkUrl).collect { st ->
                when (st) {
                    is DownloadState.InProgress ->
                        _downloads.update { it + (videoId to ItemDownload.Downloading(st.fraction)) }
                    is DownloadState.Completed -> {
                        _downloads.update { it - videoId }
                        play(st.song)
                    }
                    is DownloadState.Failed ->
                        _downloads.update { it + (videoId to ItemDownload.Failed(st.error.message ?: "Download failed")) }
                }
            }
        }
    }
}
