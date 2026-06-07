package com.youtubestream.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.youtubestream.app.ui.navigation.AppNavHost
import com.youtubestream.app.ui.theme.YoutubeStreamTheme
import com.youtubestream.app.widget.WidgetIntents

class MainActivity : ComponentActivity() {

    // Android 13+ runtime permission for the media notification. Playback works regardless.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Drives a one-shot navigation to the Player when launched from the widget body.
    private val openPlayer = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        openPlayer.value = consumeOpenPlayer(intent)

        val container = (application as App).container

        setContent {
            YoutubeStreamTheme {
                AppNavHost(
                    container = container,
                    openPlayerSignal = openPlayer.value,
                    onPlayerOpened = { openPlayer.value = false },
                )
            }
        }
    }

    // Widget tapped while the activity already exists (singleTop): re-read the extra.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPlayer.value = consumeOpenPlayer(intent)
    }

    /**
     * Reads the open-Player flag and *consumes* it (removes it from the intent) so that a later
     * config-change recreation doesn't re-navigate to the Player after the user has moved on.
     */
    private fun consumeOpenPlayer(intent: Intent?): Boolean {
        val open = intent?.getBooleanExtra(WidgetIntents.EXTRA_OPEN_PLAYER, false) == true
        if (open && intent != null) {
            intent.removeExtra(WidgetIntents.EXTRA_OPEN_PLAYER)
            setIntent(intent)
        }
        return open
    }

    companion object {
        private const val RC_OPEN_PLAYER = 200

        /**
         * PendingIntent that launches the app and asks the UI to expand the Player sheet.
         * One definition shared by the home-screen widget body and the Media3 media
         * notification, so the "open the Player" contract lives in exactly one place.
         */
        fun openPlayerPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(WidgetIntents.EXTRA_OPEN_PLAYER, true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            return PendingIntent.getActivity(
                context,
                RC_OPEN_PLAYER,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
