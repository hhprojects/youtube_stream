package com.youtubestream.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.ui.debug.DebugPlaybackScreen
import com.youtubestream.app.ui.theme.YoutubeStreamTheme

class MainActivity : ComponentActivity() {

    private lateinit var connection: PlaybackConnection

    // Android 13+ runtime permission for the media notification. Playback works regardless.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Manual wiring (Hilt deferred): one connection, app-context to avoid leaks,
        // lifecycleScope so the position loop auto-cancels when this Activity dies.
        connection = PlaybackConnection(applicationContext, lifecycleScope)
        connection.connect()

        setContent {
            YoutubeStreamTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DebugPlaybackScreen(
                        connection = connection,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        connection.release()
        super.onDestroy()
    }
}
