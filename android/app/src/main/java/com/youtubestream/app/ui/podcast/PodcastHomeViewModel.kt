package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Remote half of the home: the curated/featured show shelves + the latest-from-followed episodes. */
private data class RemoteHome(val shelves: List<PodcastShelf>, val latest: List<LatestEpisode>)

class PodcastHomeViewModel(
    private val repo: PodcastSource,
    private val downloads: PodcastDownloads,
    private val playDownloaded: (PodcastEpisode) -> Unit,
) : ViewModel() {

    private val _remote = MutableStateFlow<UiState<RemoteHome>>(UiState.Loading)
    val downloadsState: StateFlow<Map<String, ItemDownload>> = downloads.downloads

    val state: StateFlow<UiState<List<PodcastHomeSection>>> =
        combine(_remote, repo.observeContinueListening()) { remote, cont ->
            when (remote) {
                is UiState.Content ->
                    UiState.Content(buildPodcastHome(cont, remote.data.latest, remote.data.shelves))
                is UiState.Error ->
                    // Offline-friendly: still show downloaded in-progress episodes if the network half failed.
                    if (cont.isNotEmpty()) UiState.Content(buildPodcastHome(cont, emptyList(), emptyList()))
                    else UiState.Error(remote.message)
                else -> UiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _remote.value = UiState.Loading
            _remote.value = try {
                val shelves = repo.home()
                val latest = repo.latestFromShows(repo.followedShowIds())
                UiState.Content(RemoteHome(shelves, latest))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Couldn't load podcasts")
            }
        }
    }

    /** Continue listening tap: play the already-downloaded episode (resume applied by the play lambda). */
    fun onPlay(episode: PodcastEpisode) = playDownloaded(episode)

    /** Latest tap: download the (remote) episode, then it auto-plays. A lightweight show detail carries
     *  just the fields the download needs (title → showName, showId). */
    fun onDownloadAndPlay(item: LatestEpisode) {
        val lite = PodcastShowDetail(item.showId, item.showName, null, null, null, emptyList())
        downloads.download(viewModelScope, lite, item.episode)
    }
}
