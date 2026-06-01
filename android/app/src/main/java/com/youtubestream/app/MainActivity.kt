package com.youtubestream.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.youtubestream.app.ui.navigation.AppNavHost
import com.youtubestream.app.ui.theme.YoutubeStreamTheme

class MainActivity : ComponentActivity() {

    // Android 13+ runtime permission for the media notification. Playback works regardless.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val container = (application as App).container

        setContent {
            YoutubeStreamTheme {
                AppNavHost(container = container)
            }
        }
    }
}
