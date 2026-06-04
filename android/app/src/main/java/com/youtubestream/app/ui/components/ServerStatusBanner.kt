package com.youtubestream.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.network.ServerStatus

/**
 * A thin offline banner for Pi-dependent screens. Renders nothing while the server is usable
 * (REACHABLE/CHECKING); shows a reason when known-bad, with a Retry only for SERVER_UNREACHABLE
 * (DEVICE_OFFLINE recovers automatically when connectivity returns, so no manual retry is needed).
 */
@Composable
fun ServerStatusBanner(status: ServerStatus, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (status) {
        ServerStatus.DEVICE_OFFLINE -> "You're offline. Connect to Wi-Fi to search & download."
        ServerStatus.SERVER_UNREACHABLE -> "Can't reach your server — is the Pi on?"
        ServerStatus.REACHABLE, ServerStatus.CHECKING -> return   // nothing to show
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (status == ServerStatus.SERVER_UNREACHABLE) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
