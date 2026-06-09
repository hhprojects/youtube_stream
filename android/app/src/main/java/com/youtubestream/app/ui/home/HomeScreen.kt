package com.youtubestream.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.discover.DiscoverUiState
import com.youtubestream.app.ui.discover.DiscoverViewModel
import com.youtubestream.app.ui.discover.discoverSection
import com.youtubestream.app.ui.library.toPlayableTrack

@Composable
fun HomeScreen(onOpenMood: (String) -> Unit, onOpenGenre: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel { c ->
        HomeViewModel(
            observeLibrary = { c.libraryRepository.observeLibrary() },
            observeHistory = { c.playHistoryRepository.observe() },
            play = { songs, index ->
                c.playbackConnection.setQueueAndPlay(songs.map { it.toPlayableTrack() }, index)
            },
        )
    }
    val discoverVm = appViewModel { c ->
        DiscoverViewModel(
            source = c.discoveryRepository,
            reachability = c.serverReachability,
            observeLibrary = { c.libraryRepository.observeLibrary() },
            observeHistory = { c.playHistoryRepository.observe() },
            songDownloads = c.songDownloads,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val discover by discoverVm.state.collectAsStateWithLifecycle()
    val discoverDownloads by discoverVm.downloadsState.collectAsStateWithLifecycle()

    val forYouShelves = (state as? UiState.Content)?.data.orEmpty()
    val nothingToShow = state is UiState.Content && forYouShelves.isEmpty() && discover is DiscoverUiState.Hidden

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state is UiState.Error -> Text("Error: ${(state as UiState.Error).message}")
            state is UiState.Idle || (state is UiState.Loading && discover is DiscoverUiState.Loading) ->
                CircularProgressIndicator()
            nothingToShow ->
                Text("No music yet. Download a song and it'll show up here.")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(forYouShelves, key = { "foryou-" + it.id.name }) { shelf ->
                    ShelfCardRow(
                        title = shelf.title,
                        cards = shelf.songs.map { it.toCard() },
                        downloadingKeys = emptySet(),
                    ) { index -> vm.onPlay(shelf.songs, index) }
                }
                discoverSection(
                    state = discover,
                    downloadingKeys = discoverDownloads.keys,
                    onSongClick = discoverVm::onSongClick,
                    onOpenMood = onOpenMood,
                    onOpenGenre = onOpenGenre,
                )
            }
        }
    }
}

private fun com.youtubestream.app.data.local.LibrarySong.toCard() =
    ShelfCardUi(key = id, title = title, artist = artist, artworkUrl = artworkUrl)
