package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.download.PodcastDownloads
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
        combine(_remote, repo.observeContinueListening(), repo.observeFollowedShows()) { remote, cont, followed ->
            when (remote) {
                is UiState.Content ->
                    UiState.Content(buildPodcastHome(followed, cont, remote.data.latest, remote.data.shelves))
                is UiState.Error ->
                    // Offline-friendly: followed shows + downloads are local, so still render them if the network half failed.
                    if (cont.isNotEmpty() || followed.isNotEmpty())
                        UiState.Content(buildPodcastHome(followed, cont, emptyList(), emptyList()))
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

    /** Latest tap: queue the (remote) episode for download (decoupled — no auto-play; play it once it's
     *  downloaded). A lightweight show detail carries just the fields the download needs (showName, showId). */
    fun onDownload(item: LatestEpisode) {
        val lite = PodcastShowDetail(item.showId, item.showName, null, null, null, emptyList())
        downloads.enqueue(lite, item.episode)
    }
}
