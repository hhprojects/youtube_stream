package com.youtubestream.app.ui.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.home.ShelfCardRow
import com.youtubestream.app.ui.home.ShelfCardUi
import com.youtubestream.app.ui.search.ItemDownload

/** Podcast tab landing: Continue listening + Latest from your shows + curated/featured show shelves. */
@Composable
fun PodcastHomeScreen(
    onShowClick: (showId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel { c ->
        val play: (PodcastEpisode) -> Unit = { ep ->
            c.playbackConnection.setQueueAndPlay(
                listOf(ep.toPlayableTrack()), 0, if (ep.isFinished) 0L else ep.resumePositionMs,
            )
        }
        PodcastHomeViewModel(
            repo = c.podcastRepository,
            downloads = PodcastDownloads(repo = c.podcastRepository, play = play),
            playDownloaded = play,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val downloads by vm.downloadsState.collectAsStateWithLifecycle()

    when (val s = state) {
        is UiState.Loading, UiState.Idle ->
            Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error ->
            Box(modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::reload) { Text("Retry") }
                }
            }
        is UiState.Content -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(s.data) { section ->
                when (section) {
                    is PodcastHomeSection.ContinueListening -> ShelfCardRow(
                        title = "Continue listening",
                        cards = section.episodes.map { ShelfCardUi(it.id, it.title, it.showName, it.artworkUrl) },
                        downloadingKeys = emptySet(),
                        onCardClick = { i -> vm.onPlay(section.episodes[i]) },
                    )
                    is PodcastHomeSection.Latest -> ShelfCardRow(
                        title = "Latest from your shows",
                        cards = section.items.map { ShelfCardUi(it.episode.videoId, it.episode.title, it.showName, it.episode.artworkUrl) },
                        downloadingKeys = downloads.filterValues { it is ItemDownload.Downloading }.keys,
                        onCardClick = { i -> vm.onDownloadAndPlay(section.items[i]) },
                    )
                    is PodcastHomeSection.ShowShelf -> ShelfCardRow(
                        title = section.label,
                        cards = section.shows.map { ShelfCardUi(it.showId, it.title, it.author ?: "", it.artworkUrl) },
                        downloadingKeys = emptySet(),
                        onCardClick = { i -> onShowClick(section.shows[i].showId) },
                    )
                }
            }
        }
    }
}
