package com.youtubestream.app.ui.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    val all = songs + pods
    if (all.isEmpty()) return

    var open by remember { mutableStateOf(false) }
    BadgedBox(badge = { Badge { Text(all.size.toString()) } }) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.Download, contentDescription = "Downloads")
        }
    }
    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Downloads", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                all.forEach { d ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(d.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            when (val s = d.status) {
                                ItemDownload.Queued -> Text("Queued", style = MaterialTheme.typography.bodySmall)
                                is ItemDownload.Downloading ->
                                    LinearProgressIndicator(progress = { s.fraction }, modifier = Modifier.fillMaxWidth())
                                is ItemDownload.Failed -> Text("Failed", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { songDownloads.cancel(d.key); podcastDownloads.cancel(d.key) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel download")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
