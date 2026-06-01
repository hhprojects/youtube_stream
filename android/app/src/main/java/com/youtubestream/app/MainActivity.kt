package com.youtubestream.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.ui.navigation.AppNavHost
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

        val container = (application as App).container

        setContent {
            YoutubeStreamTheme {
                AppNavHost(connection = connection, container = container)
            }
        }
    }

    override fun onDestroy() {
        connection.release()
        super.onDestroy()
    }
}
