package com.youtubestream.app.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodDetail
import com.youtubestream.app.data.repository.DiscoverySource
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SongArtwork
import com.youtubestream.app.ui.library.toPlayableTrack
import com.youtubestream.app.ui.search.ItemDownload

/**
 * A remote {title, songs} list with tap->download->play. Used for both mood-detail and genre-chart.
 * [load] fetches via the app's DiscoverySource; [titleOverride] (when non-null) is shown immediately
 * (genre charts pass the cleaned genre name), else the loaded title, falling back to [fallbackTitle].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryListScreen(
    load: suspend (DiscoverySource) -> MoodDetail,
    fallbackTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleOverride: String? = null,
) {
    val vm = appViewModel { c ->
        DiscoveryListViewModel(
            load = { load(c.discoveryRepository) },
            songDownloads = c.songDownloads,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val downloads by vm.downloadsState.collectAsStateWithLifecycle()
    val title = titleOverride
        ?: (state as? UiState.Content)?.data?.title?.takeIf { it.isNotBlank() }
        ?: fallbackTitle

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(s.message)
                is UiState.Content -> if (s.data.songs.isEmpty()) {
                    Text("Nothing here right now.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(s.data.songs, key = { it.videoId }) { song ->
                            DiscoverySongRow(song, downloads[song.videoId]) { vm.onSongClick(song) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverySongRow(song: DiscoverySong, download: ItemDownload?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            SongArtwork(song.thumbnailUrl, size = 48.dp)
            if (download is ItemDownload.Downloading) CircularProgressIndicator(Modifier.size(20.dp))
        }
        Column(Modifier.padding(end = 8.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}
