package com.youtubestream.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youtubestream.app.lyrics.LrcParser
import com.youtubestream.app.lyrics.LyricsResult

/**
 * Renders the four lyrics states. Driven by [positionMs] (already ticked in PlayerUiState) for the synced
 * highlight + auto-scroll. Lives inside the player's Surface, so un-tinted Text inherits onSurface; the
 * highlight/dim colors are taken explicitly from colorScheme (dark-mode-correct).
 */
@Composable
fun LyricsView(result: LyricsResult?, positionMs: Long, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (result) {
            null -> CircularProgressIndicator()
            is LyricsResult.Synced -> SyncedLyrics(result, positionMs)
            is LyricsResult.Plain -> Text(
                result.text,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LyricsResult.None, LyricsResult.Unavailable -> Text(
                "No lyrics found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SyncedLyrics(synced: LyricsResult.Synced, positionMs: Long) {
    val listState = rememberLazyListState()
    val current = remember(positionMs, synced.lines) {
        LrcParser.currentLineIndex(positionMs, synced.lines)
    }
    // Keep the active line in view, biased toward the upper-middle of the panel.
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem(current.coerceAtLeast(0), scrollOffset = -220)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(synced.lines) { line ->
            val isCurrent = synced.lines.getOrNull(current) === line
            Text(
                line.text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
