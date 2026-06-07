package com.youtubestream.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.network.ReachabilitySource
import com.youtubestream.app.data.network.ServerStatus
import com.youtubestream.app.data.repository.Downloader
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-result download status a row renders. `Completed` isn't here — a finished id moves to [SearchViewModel.downloadedIds]. */
sealed interface ItemDownload {
    data class Downloading(val fraction: Float) : ItemDownload
    data class Failed(val message: String) : ItemDownload
}

class SearchViewModel(
    private val repo: SearchRepository,
    private val downloader: Downloader,
    private val library: LibraryRepository,
    private val reachability: ReachabilitySource,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Idle)
    val state: StateFlow<UiState<List<SearchResult>>> = _state.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, ItemDownload>>(emptyMap())
    /** id → in-flight/failed download. Absent means "not downloading" (idle, or already done). */
    val downloads: StateFlow<Map<String, ItemDownload>> = _downloads.asStateFlow()

    /** videoIds already in the local library — rows render these as "downloaded". (id is the filename now.) */
    val downloadedIds: StateFlow<Set<String>> = library.observeLibrary()
        .map { songs -> songs.mapNotNull { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** App-wide Pi reachability — the screen shows a banner and gates controls on this. */
    val status: StateFlow<ServerStatus> = reachability.status

    /** Probe on screen entry so the gate reflects the Pi before the user acts. */
    fun onEnter() { viewModelScope.launch { reachability.probe() } }

    private var searchJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()   // a newer search supersedes the last — don't let a stale result win
        searchJob = viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repo.search(query.trim()))
            } catch (e: CancellationException) {
                throw e       // cancelled by a newer search — not an error to show
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun download(result: SearchResult) {
        if (_downloads.value[result.id] is ItemDownload.Downloading) return   // ignore double-taps
        // Mark "downloading" immediately so the row reacts before the (slow) yt-dlp POST returns,
        // and so the guard above blocks repeat taps during that window.
        _downloads.update { it + (result.id to ItemDownload.Downloading(0f)) }
        viewModelScope.launch {
            downloader.download(result.id, result.title, result.thumbnailUrl).collect { st ->
                _downloads.update { current ->
                    when (st) {
                        is DownloadState.InProgress ->
                            current + (result.id to ItemDownload.Downloading(st.fraction))
                        is DownloadState.Completed ->
                            current - result.id   // now appears in downloadedIds via the Room flow
                        is DownloadState.Failed ->
                            current + (result.id to ItemDownload.Failed(st.error.message ?: "Download failed"))
                    }
                }
            }
        }
    }
}
