package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.repository.PodcastDownloadState
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-episode (keyed by videoId) tap→download→play, mirroring DiscoveryDownloads. */
class PodcastDownloads(
    private val repo: PodcastSource,
    private val play: (PodcastEpisode) -> Unit,
) {
    private val _downloads = MutableStateFlow<Map<String, ItemDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, ItemDownload>> = _downloads.asStateFlow()

    fun download(scope: CoroutineScope, show: PodcastShowDetail, episode: PodcastEpisodeItem) {
        if (_downloads.value[episode.videoId] is ItemDownload.Downloading) return   // ignore double-taps
        _downloads.update { it + (episode.videoId to ItemDownload.Downloading(0f)) }
        scope.launch {
            repo.downloadEpisode(show, episode).collect { st ->
                when (st) {
                    is PodcastDownloadState.InProgress ->
                        _downloads.update { it + (episode.videoId to ItemDownload.Downloading(st.fraction)) }
                    is PodcastDownloadState.Completed -> { _downloads.update { it - episode.videoId }; play(st.episode) }
                    is PodcastDownloadState.Failed ->
                        _downloads.update { it + (episode.videoId to ItemDownload.Failed(st.error.message ?: "Download failed")) }
                }
            }
        }
    }
}
