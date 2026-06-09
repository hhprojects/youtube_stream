package com.youtubestream.app.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.repository.PodcastSource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.download.PodcastDownloads
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShowDetailViewModel(
    private val showId: String,
    private val repo: PodcastSource,
    private val downloads: PodcastDownloads,
    private val play: (PodcastEpisode) -> Unit,   // same shape PodcastDownloads takes; unified in the screen
) : ViewModel() {

    private val _detail = MutableStateFlow<UiState<PodcastShowDetail>>(UiState.Loading)
    val detail: StateFlow<UiState<PodcastShowDetail>> = _detail.asStateFlow()

    val isFollowing: StateFlow<Boolean> =
        repo.observeIsFollowing(showId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadsState: StateFlow<Map<String, ItemDownload>> = downloads.downloads

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** One-shot delete errors (e.g. the Pi delete failed) — the screen Toasts these. */
    val errors: SharedFlow<String> = _errors

    /** Merged episode rows: remote episodes folded with the locally-downloaded set (reactive). */
    val rows: StateFlow<List<EpisodeRowUi>> =
        combine(_detail, repo.observeDownloadedEpisodes()) { d, local ->
            if (d is UiState.Content) mergeEpisodeRows(d.data.episodes, local) else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _detail.value = UiState.Loading
            _detail.value = try { UiState.Content(repo.show(showId)) }
            catch (e: Exception) { UiState.Error(e.message ?: "Couldn't load show") }
        }
    }

    fun toggleFollow() {
        val d = (_detail.value as? UiState.Content)?.data ?: return
        viewModelScope.launch { if (isFollowing.value) repo.unfollow(showId) else repo.follow(d) }
    }

    fun onDownload(videoId: String) {
        val d = (_detail.value as? UiState.Content)?.data ?: return
        val ep = d.episodes.firstOrNull { it.videoId == videoId } ?: return
        downloads.enqueue(d, ep)
    }

    /** Play an already-downloaded episode by its local id (= filename). The play lambda applies resume. */
    fun onPlayDownloaded(localId: String) {
        viewModelScope.launch { repo.getEpisode(localId)?.let(play) }
    }

    /** Delete just the local copy (file + Room row). */
    fun onDeleteDownload(localId: String) {
        viewModelScope.launch { repo.getEpisode(localId)?.let { repo.deleteEpisode(it) } }
    }

    /** Delete the local copy AND the Pi copy. Pi first: on failure, the local copy is kept + an error shown. */
    fun onDeleteEverywhere(localId: String) {
        viewModelScope.launch {
            val ep = repo.getEpisode(localId) ?: return@launch
            try {
                repo.deleteEpisodeEverywhere(ep)
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Couldn't delete from the Pi")
            }
        }
    }
}
