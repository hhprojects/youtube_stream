package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.download.PodcastDownloads
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Podcast search with two independent sections: podcast-filtered Shows (tap → show detail) and
 * unfiltered YouTube Videos (tap → audio-only episode download). Each section loads and fails on
 * its own, so a dead Pi search still leaves the other section usable.
 */
class PodcastSearchViewModel(
    private val repo: PodcastSource,
    private val videoSearch: suspend (String) -> List<SearchResult>,
    private val downloads: PodcastDownloads,
) : ViewModel() {

    private val _shows = MutableStateFlow<UiState<List<PodcastShowCard>>>(UiState.Idle)
    val shows: StateFlow<UiState<List<PodcastShowCard>>> = _shows.asStateFlow()

    private val _videos = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Idle)
    val videos: StateFlow<UiState<List<SearchResult>>> = _videos.asStateFlow()

    /** videoId → Queued/Downloading/Failed, for the video rows' trailing state. */
    val videoDownloads: StateFlow<Map<String, ItemDownload>> = downloads.downloads

    /** videoIds already downloaded as episodes — drives the ✓ state and makes re-taps no-ops. */
    val downloadedVideoIds: StateFlow<Set<String>> =
        repo.observeDownloadedEpisodes()
            .map { eps -> eps.map { it.videoId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun search(rawQuery: String) {
        val q = rawQuery.trim()
        if (q.isBlank()) {
            _shows.value = UiState.Idle
            _videos.value = UiState.Idle
            return
        }
        _shows.value = UiState.Loading
        _videos.value = UiState.Loading
        viewModelScope.launch {
            _shows.value = try {
                UiState.Content(repo.search(q))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Search failed")
            }
        }
        viewModelScope.launch {
            _videos.value = try {
                UiState.Content(videoSearch(q))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    /** Queue an audio-only download of [video] as a podcast episode, grouped under its channel. */
    fun onVideoTap(video: SearchResult) {
        val (show, episode) = videoAsEpisode(video)
        downloads.enqueue(show, episode)
    }
}
