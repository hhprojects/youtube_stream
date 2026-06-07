package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodCategory
import com.youtubestream.app.data.network.ServerStatus

/** One horizontal Discover row of remote songs. */
data class DiscoverShelf(val id: String, val title: String, val songs: List<DiscoverySong>)

/**
 * Per-shelf result: a null field = that shelf's fetch failed (or had no seed) and is omitted.
 * This is NOT "Hidden" — Hidden is reserved for not-reachable.
 */
data class DiscoverContent(
    val trending: DiscoverShelf?,
    val related: DiscoverShelf?,
    val moods: List<MoodCategory>?,
)

sealed interface DiscoverUiState {
    data object Hidden : DiscoverUiState     // not reachable (offline / server unreachable)
    data object Loading : DiscoverUiState    // reachable/checking, fetch in flight → skeleton
    data class Content(val content: DiscoverContent) : DiscoverUiState
}

enum class DiscoverVisibility { SHOW, SKELETON, HIDDEN }

/** Pure: server status → whether/how to show Discover. */
fun discoverVisibility(status: ServerStatus): DiscoverVisibility = when (status) {
    ServerStatus.REACHABLE -> DiscoverVisibility.SHOW
    ServerStatus.CHECKING -> DiscoverVisibility.SKELETON
    ServerStatus.DEVICE_OFFLINE, ServerStatus.SERVER_UNREACHABLE -> DiscoverVisibility.HIDDEN
}
