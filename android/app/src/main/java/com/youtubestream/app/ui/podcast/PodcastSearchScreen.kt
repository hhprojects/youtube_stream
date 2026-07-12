package com.youtubestream.app.ui.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.components.SongRow
import com.youtubestream.app.ui.search.ItemDownload

/** Focused full-screen podcast search, launched from the Podcast tab's app-bar magnifier. Two
 *  independent sections: podcast-filtered Shows (tap → ShowDetailScreen) and unfiltered YouTube
 *  Videos (tap → audio-only episode download, grouped under the channel). */
@Composable
fun PodcastSearchScreen(
    onBack: () -> Unit,
    onShowClick: (showId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel { PodcastSearchViewModel(it.podcastRepository, it.searchRepository::search, it.podcastDownloads) }
    val shows by vm.shows.collectAsStateWithLifecycle()
    val videos by vm.videos.collectAsStateWithLifecycle()
    val downloadedIds by vm.downloadedVideoIds.collectAsStateWithLifecycle()
    val videoDownloads by vm.videoDownloads.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // The page exists to type a query — open with the field focused and the keyboard up.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun submit(q: String) {
        query = q
        vm.search(q)
        focusManager.clearFocus()
    }

    Column(modifier = modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search shows & videos") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(query) }),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close search")
            }
        }

        // Page-level states: prompt before the first search; one "No results" when BOTH sections
        // came back empty (per the spec) — otherwise the two sections render independently.
        val bothEmpty = (shows as? UiState.Content)?.data?.isEmpty() == true &&
            (videos as? UiState.Content)?.data?.isEmpty() == true
        if (shows is UiState.Idle && videos is UiState.Idle) {
            Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Text("Search for shows & videos")
            }
        } else if (bothEmpty) {
            Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Text("No results")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            ) {
                sectionHeader("Shows")
                showsSection(shows, onShowClick, onRetry = { vm.search(query) })
                sectionHeader("Videos")
                videosSection(
                    state = videos,
                    downloadedIds = downloadedIds,
                    downloads = videoDownloads,
                    onVideoTap = vm::onVideoTap,
                    onRetry = { vm.search(query) },
                )
            }
        }
    }
}

private fun LazyListScope.sectionHeader(label: String) {
    item(key = "header:$label") {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    }
}

private fun LazyListScope.showsSection(
    state: UiState<List<PodcastShowCard>>,
    onShowClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        is UiState.Idle -> Unit
        is UiState.Loading -> item(key = "shows:loading") { SectionLoading() }
        is UiState.Error -> item(key = "shows:error") { SectionError(state.message, onRetry) }
        is UiState.Content -> if (state.data.isEmpty()) {
            item(key = "shows:empty") { Text("No shows found") }
        } else {
            items(state.data, key = { "show:${it.showId}" }) { show ->
                ShowResultRow(show = show, onClick = { onShowClick(show.showId) })
            }
        }
    }
}

private fun LazyListScope.videosSection(
    state: UiState<List<SearchResult>>,
    downloadedIds: Set<String>,
    downloads: Map<String, ItemDownload>,
    onVideoTap: (SearchResult) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        is UiState.Idle -> Unit
        is UiState.Loading -> item(key = "videos:loading") { SectionLoading() }
        is UiState.Error -> item(key = "videos:error") { SectionError(state.message, onRetry) }
        is UiState.Content -> if (state.data.isEmpty()) {
            item(key = "videos:empty") { Text("No videos found") }
        } else {
            items(state.data, key = { "video:${it.id}" }) { video ->
                val downloaded = video.id in downloadedIds
                SongRow(
                    title = video.title,
                    artist = listOfNotNull(
                        video.channel.takeIf { it.isNotBlank() },
                        video.durationSeconds?.let(::formatEpisodeDuration),
                    ).joinToString(" • "),
                    artworkUrl = video.thumbnailUrl,
                    onClick = { if (!downloaded) onVideoTap(video) },
                    trailing = {
                        when (downloads[video.id]) {
                            is ItemDownload.Queued, is ItemDownload.Downloading ->
                                CircularProgressIndicator(Modifier.size(24.dp))
                            is ItemDownload.Failed -> IconButton(onClick = { onVideoTap(video) }) {
                                Icon(Icons.Filled.Download, contentDescription = "Retry download")
                            }
                            else -> if (downloaded) {
                                Icon(
                                    Icons.Filled.DownloadDone,
                                    contentDescription = "Downloaded",
                                    modifier = Modifier.padding(12.dp),
                                )
                            } else {
                                IconButton(onClick = { onVideoTap(video) }) {
                                    Icon(Icons.Filled.Download, contentDescription = "Download as episode")
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionLoading() {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SectionError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Error: $message")
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ShowResultRow(show: PodcastShowCard, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = show.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(show.title, maxLines = 1)
            show.author?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 1) }
        }
    }
}
