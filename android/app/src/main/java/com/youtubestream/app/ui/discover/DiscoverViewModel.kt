package com.youtubestream.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.network.ReachabilitySource
import com.youtubestream.app.data.repository.DiscoverySource
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/**
 * Drives the Discover section. The reachability status is the source flow: a status change cancels any
 * in-flight fetch (flatMapLatest). Each shelf is fetched in parallel and degrades independently —
 * a failed fetch (or no related-seed) nulls only that field. Hidden is used ONLY when not reachable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModel(
    private val source: DiscoverySource,
    reachability: ReachabilitySource,
    private val observeLibrary: () -> Flow<List<LibrarySong>>,
    private val observeHistory: () -> Flow<List<PlayEvent>>,
    private val downloads: DiscoveryDownloads,
    private val region: () -> String = { Locale.getDefault().country.ifBlank { "US" } },
) : ViewModel() {

    val downloadsState: StateFlow<Map<String, ItemDownload>> = downloads.downloads

    val state: StateFlow<DiscoverUiState> = reachability.status
        .map { discoverVisibility(it) }
        .distinctUntilChanged()
        .flatMapLatest { vis ->
            when (vis) {
                DiscoverVisibility.HIDDEN -> flowOf(DiscoverUiState.Hidden)
                DiscoverVisibility.SKELETON -> flowOf(DiscoverUiState.Loading)
                DiscoverVisibility.SHOW -> flow {
                    emit(DiscoverUiState.Loading)
                    emit(DiscoverUiState.Content(loadContent()))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState.Loading)

    private suspend fun loadContent(): DiscoverContent = coroutineScope {
        val library = observeLibrary().first()
        val history = observeHistory().first()
        val seedId = selectRelatedSeed(history, library)   // seedId is a videoId
        val seedTitle = seedId?.let { vid -> library.firstOrNull { it.videoId == vid }?.title }

        val trendingD = async { runCatching { source.trending(region()) }.getOrNull() }
        val relatedD = async { seedId?.let { runCatching { source.related(it) }.getOrNull() } }
        val moodsD = async { runCatching { source.moods() }.getOrNull() }
        val genreChartsD = async { runCatching { source.genreCharts("US") }.getOrNull() }

        val trending = trendingD.await()?.takeIf { it.isNotEmpty() }
            ?.let { DiscoverShelf("trending", "Trending now", it) }
        val related = relatedD.await()?.takeIf { it.isNotEmpty() }
            ?.let {
                val title = seedTitle?.let { t -> "Because you played \"$t\"" } ?: "Recommended for you"
                DiscoverShelf("related", title, it)
            }
        val moods = moodsD.await()?.takeIf { it.isNotEmpty() }
        val genreCharts = genreChartsD.await()?.takeIf { it.isNotEmpty() }
        DiscoverContent(trending = trending, related = related, moods = moods, genreCharts = genreCharts)
    }

    fun onSongClick(song: DiscoverySong) =
        downloads.download(viewModelScope, song.videoId, song.title, song.thumbnailUrl)
}
