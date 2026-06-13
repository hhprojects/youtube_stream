package com.youtubestream.app.ui.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtubestream.app.ui.search.ItemDownload

/**
 * App-bar affordance: a badge with the count of active downloads (songs + episodes, from the two
 * app-scoped queues); tap opens a sheet listing them with progress + per-item cancel. Renders nothing
 * while no download is active. `cancel` is called on both queues — it's a no-op on the one that doesn't
 * own the key, so there's no need to track which queue a row came from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsIndicator(songDownloads: SongDownloads, podcastDownloads: PodcastDownloads) {
    val songs by songDownloads.queue.active.collectAsStateWithLifecycle()
    val pods by podcastDownloads.queue.active.collectAsStateWithLifecycle()
    // Tag each row with its kind so the sheet can label Song vs Podcast (cancel/retry are no-ops on the
    // queue that doesn't own the key, so we don't need to route by kind — just label it).
    val rows = songs.map { it to false } + pods.map { it to true }
    if (rows.isEmpty()) return

    var open by remember { mutableStateOf(false) }
    BadgedBox(badge = { Badge { Text(rows.size.toString()) } }) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.Download, contentDescription = "Downloads")
        }
    }
    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Downloads", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    rows.forEach { (d, _) -> songDownloads.cancel(d.key); podcastDownloads.cancel(d.key) }
                }) { Text("Cancel all") }
            }
            // LazyColumn (not a forEach in a Column) so a long queue scrolls instead of overflowing the sheet.
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(horizontal = 16.dp)) {
                items(rows, key = { (d, _) -> d.key }) { (d, podcast) ->
                    DownloadRow(
                        download = d,
                        podcast = podcast,
                        onCancel = { songDownloads.cancel(d.key); podcastDownloads.cancel(d.key) },
                        onRetry = { songDownloads.retry(d.key); podcastDownloads.retry(d.key) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    download: ActiveDownload,
    podcast: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val type = if (podcast) "Podcast" else "Song"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(download.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (val s = download.status) {
                ItemDownload.Queued ->
                    Text("$type · Queued", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                is ItemDownload.Downloading -> {
                    LinearProgressIndicator(progress = { s.fraction }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "$type · ${(s.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ItemDownload.Failed ->
                    Text("$type · Failed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        if (download.status is ItemDownload.Failed) {
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel download")
        }
    }
}
