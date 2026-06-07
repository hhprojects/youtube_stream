package com.youtubestream.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodCategory
import com.youtubestream.app.ui.home.ShelfCardRow
import com.youtubestream.app.ui.home.ShelfCardUi

private fun DiscoverySong.toCard() = ShelfCardUi(key = videoId, title = title, artist = artist, artworkUrl = thumbnailUrl)

/** Appends the Discover rows to the Home LazyColumn. Renders nothing when Hidden. */
fun LazyListScope.discoverSection(
    state: DiscoverUiState,
    downloadingKeys: Set<String>,
    onSongClick: (DiscoverySong) -> Unit,
    onOpenMood: (String) -> Unit,
) {
    when (state) {
        is DiscoverUiState.Hidden -> Unit
        is DiscoverUiState.Loading -> item(key = "discover-skeleton") { DiscoverSkeleton() }
        is DiscoverUiState.Content -> {
            val c = state.content
            item(key = "discover-header") {
                Text(
                    "Discover",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            c.trending?.let { sh ->
                item(key = "discover-trending") {
                    ShelfCardRow(sh.title, sh.songs.map { it.toCard() }, downloadingKeys) { i -> onSongClick(sh.songs[i]) }
                }
            }
            c.related?.let { sh ->
                item(key = "discover-related") {
                    ShelfCardRow(sh.title, sh.songs.map { it.toCard() }, downloadingKeys) { i -> onSongClick(sh.songs[i]) }
                }
            }
            c.moods?.let { cats ->
                item(key = "discover-moods") { MoodChipRow(cats, onOpenMood) }
            }
        }
    }
}

@Composable
private fun MoodChipRow(categories: List<MoodCategory>, onOpenMood: (String) -> Unit) {
    Text(
        "Moods & genres",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.key }) { cat ->
            AssistChip(onClick = { onOpenMood(cat.key) }, label = { Text(cat.title) })
        }
    }
}

@Composable
private fun DiscoverSkeleton() {
    Text(
        "Finding music for you…",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}
