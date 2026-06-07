package com.youtubestream.app.ui.podcast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
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
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SongArtwork
import com.youtubestream.app.ui.components.SongRow
import com.youtubestream.app.ui.search.ItemDownload

/** Show detail: header (art, title, author, follow, notes) + the episode list with download/play. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel { c ->
        // Define the play action ONCE and share it: fresh-download autoplay (PodcastDownloads) and
        // already-downloaded replay (the VM) both use it. Finished episodes restart from 0 — else they'd
        // resume at ~95% and instantly re-complete. The lambda calls PlaybackConnection (no Media3 here).
        val play: (PodcastEpisode) -> Unit = { ep ->
            c.playbackConnection.setQueueAndPlay(
                listOf(ep.toPlayableTrack()),
                0,
                if (ep.isFinished) 0L else ep.resumePositionMs,
            )
        }
        ShowDetailViewModel(
            showId = showId,
            repo = c.podcastRepository,
            downloads = PodcastDownloads(repo = c.podcastRepository, play = play),
            play = play,
        )
    }

    val detail by vm.detail.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val following by vm.isFollowing.collectAsStateWithLifecycle()
    val downloads by vm.downloadsState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text((detail as? UiState.Content)?.data?.title ?: "Show", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        when (val d = detail) {
            is UiState.Loading, UiState.Idle ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Button(onClick = vm::reload) { Text("Retry") } }
            is UiState.Content -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    Column {
                        SongArtwork(d.data.artworkUrl, size = 120.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(d.data.title, style = MaterialTheme.typography.titleLarge)
                        d.data.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = vm::toggleFollow) { Text(if (following) "Following" else "Follow") }
                        d.data.description?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
                items(rows, key = { it.videoId }) { row ->
                    SongRow(
                        title = row.title,
                        artist = "",
                        artworkUrl = row.artworkUrl,
                        onClick = {
                            if (row.downloaded) row.localId?.let(vm::onPlayDownloaded) else vm.onDownload(row.videoId)
                        },
                        trailing = {
                            when (downloads[row.videoId]) {
                                is ItemDownload.Downloading -> CircularProgressIndicator(Modifier.size(24.dp))
                                is ItemDownload.Failed -> IconButton(onClick = { vm.onDownload(row.videoId) }) {
                                    Icon(Icons.Filled.Download, contentDescription = "Retry download")
                                }
                                else -> if (row.downloaded) {
                                    IconButton(onClick = { row.localId?.let(vm::onPlayDownloaded) }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                    }
                                } else {
                                    IconButton(onClick = { vm.onDownload(row.videoId) }) {
                                        Icon(Icons.Filled.Download, contentDescription = "Download")
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
