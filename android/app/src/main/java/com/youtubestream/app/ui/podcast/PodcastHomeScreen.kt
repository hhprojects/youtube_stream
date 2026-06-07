package com.youtubestream.app.ui.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.Spacer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel
import com.youtubestream.app.ui.home.ShelfCardRow
import com.youtubestream.app.ui.home.ShelfCardUi

/** Podcast tab landing: curated shelves of show cards. Tapping a card opens the show. */
@Composable
fun PodcastHomeScreen(
    onShowClick: (showId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel { c -> PodcastHomeViewModel(c.podcastRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

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
            items(s.data, key = { it.label }) { shelf ->
                ShelfCardRow(
                    title = shelf.label,
                    cards = shelf.shows.map { ShelfCardUi(it.showId, it.title, it.author ?: "", it.artworkUrl) },
                    downloadingKeys = emptySet(),
                    onCardClick = { i -> onShowClick(shelf.shows[i].showId) },
                )
            }
        }
    }
}
