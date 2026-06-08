package com.youtubestream.app.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared song row: artwork + title/artist, with a caller-supplied [trailing] slot for actions
 * (edit / delete / add-to-playlist / drag-handle). In multi-select mode it instead shows a leading
 * checkbox and hides the trailing slot, so "tap row toggles" never competes with a trailing icon.
 * [onLongClick] (null = disabled) lets a screen start selection by long-press where no other gesture
 * owns it (All Songs); playlist detail leaves it null because long-press is the drag-reorder gesture.
 */
@Composable
fun SongRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    inSelectionMode: Boolean = false,
    selected: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inSelectionMode) {
                Checkbox(checked = selected, onCheckedChange = null)   // row click drives the toggle
                Spacer(Modifier.width(12.dp))
            }
            SongArtwork(artworkUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1)
                Text(artist, maxLines = 1)
            }
            if (!inSelectionMode) trailing()
        }
    }
}
