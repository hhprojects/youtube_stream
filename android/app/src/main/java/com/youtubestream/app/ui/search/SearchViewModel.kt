package com.youtubestream.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.network.ReachabilitySource
import com.youtubestream.app.data.network.ServerStatus
import com.youtubestream.app.data.repository.LibraryRepository
import com.youtubestream.app.data.repository.RecentSearchRepository
import com.youtubestream.app.data.repository.SearchRepository
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.download.SongDownloads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Per-result download status a row renders. `Completed` isn't here — a finished id moves to [SearchViewModel.downloadedIds]. */
sealed interface ItemDownload {
    data object Queued : ItemDownload
    data class Downloading(val fraction: Float) : ItemDownload
    data class Failed(val message: String) : ItemDownload
}

class SearchViewModel(
    private val repo: SearchRepository,
    private val songDownloads: SongDownloads,
    private val library: LibraryRepository,
    private val reachability: ReachabilitySource,
    private val recentsRepo: RecentSearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Idle)
    val state: StateFlow<UiState<List<SearchResult>>> = _state.asStateFlow()

    /** id → queued/in-flight/failed download, from the app-scoped queue (survives leaving the screen). */
    val downloads: StateFlow<Map<String, ItemDownload>> = songDownloads.downloads

    /** videoIds already in the local library — rows render these as "downloaded". (id is the filename now.) */
    val downloadedIds: StateFlow<Set<String>> = library.observeLibrary()
        .map { songs -> songs.mapNotNull { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Recent queries, newest first — shown as the empty-state of the search page. */
    val recents: StateFlow<List<String>> = recentsRepo.observe(RECENTS_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** App-wide Pi reachability — the screen shows a banner and gates controls on this. */
    val status: StateFlow<ServerStatus> = reachability.status

    /** Probe on screen entry so the gate reflects the Pi before the user acts. */
    fun onEnter() { viewModelScope.launch { reachability.probe() } }

    private var searchJob: Job? = null

    fun search(query: String) {
        val q = normalizeQuery(query) ?: return       // ignore blank/whitespace-only
        viewModelScope.launch { recentsRepo.save(q) } // remember it (REPLACE handles dedup + recency)
        searchJob?.cancel()                           // a newer search supersedes the last
        searchJob = viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repo.search(q))
            } catch (e: CancellationException) {
                throw e       // cancelled by a newer search — not an error to show
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun removeRecent(query: String) { viewModelScope.launch { recentsRepo.remove(query) } }

    fun clearRecents() { viewModelScope.launch { recentsRepo.clear() } }

    /** Queue the download on the app-scoped queue (dedupe + sequential processing live there), so it
     *  keeps running after the user leaves the Search screen. */
    fun download(result: SearchResult) = songDownloads.enqueue(result)

    private companion object { const val RECENTS_LIMIT = 8 }
}
