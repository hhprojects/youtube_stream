package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastFreshShelf
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Remote half of the home: the curated/featured show shelves + the latest-from-followed episodes. */
private data class RemoteHome(val shelves: List<PodcastShelf>, val latest: List<LatestEpisode>)

class PodcastHomeViewModel(
    private val repo: PodcastSource,
    private val downloads: PodcastDownloads,
    private val playDownloaded: (PodcastEpisode) -> Unit,
) : ViewModel() {

    private val _remote = MutableStateFlow<UiState<RemoteHome>>(UiState.Loading)
    private val _mode = MutableStateFlow(PodcastHomeMode.Popular)
    val mode: StateFlow<PodcastHomeMode> = _mode
    private val _freshShelves = MutableStateFlow<List<PodcastFreshShelf>>(emptyList())
    private val _freshLoading = MutableStateFlow(false)
    val freshLoading: StateFlow<Boolean> = _freshLoading
    private var freshRequested = false
    val downloadsState: StateFlow<Map<String, ItemDownload>> = downloads.downloads

    val state: StateFlow<UiState<List<PodcastHomeSection>>> =
        combine(_mode, _remote, repo.observeContinueListening(), repo.observeFollowedShows(), _freshShelves) {
            mode, remote, cont, followed, fresh ->
            when (remote) {
                is UiState.Content ->
                    UiState.Content(buildPodcastHome(mode, followed, cont, remote.data.shelves, remote.data.latest, fresh))
                is UiState.Error ->
                    // Offline-friendly: followed shows + downloads are local, so still render chrome + toggle.
                    if (cont.isNotEmpty() || followed.isNotEmpty())
                        UiState.Content(buildPodcastHome(mode, followed, cont, emptyList(), emptyList(), fresh))
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

    fun setMode(newMode: PodcastHomeMode) {
        _mode.value = newMode
        if (newMode == PodcastHomeMode.Newest && !freshRequested) {
            freshRequested = true
            loadFresh()
        }
    }

    private fun loadFresh() {
        viewModelScope.launch {
            _freshLoading.value = true
            try {
                _freshShelves.value = repo.fresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                freshRequested = false   // allow a retry on the next switch to Newest
            } finally {
                _freshLoading.value = false
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
