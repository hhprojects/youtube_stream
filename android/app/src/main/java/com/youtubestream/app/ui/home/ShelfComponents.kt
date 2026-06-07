package com.youtubestream.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youtubestream.app.ui.components.SongArtwork

/** Source-agnostic card model: both For You (local) and Discover (remote) map onto this. */
data class ShelfCardUi(
    val key: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
)

@Composable
fun ShelfCard(card: ShelfCardUi, downloading: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.width(140.dp).padding(8.dp)) {
            Box(contentAlignment = Alignment.Center) {
                SongArtwork(card.artworkUrl, size = 124.dp)
                if (downloading) CircularProgressIndicator(Modifier.size(32.dp))
            }
            Text(card.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(card.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** One titled horizontal row of cards. [onCardClick] gets the tapped index. */
@Composable
fun ShelfCardRow(
    title: String,
    cards: List<ShelfCardUi>,
    downloadingKeys: Set<String>,
    onCardClick: (Int) -> Unit,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(cards, key = { _, c -> c.key }) { i, c ->
                ShelfCard(c, downloading = c.key in downloadingKeys) { onCardClick(i) }
            }
        }
    }
}
