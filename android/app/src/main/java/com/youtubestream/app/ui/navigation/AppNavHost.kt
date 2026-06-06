package com.youtubestream.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import android.widget.Toast
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youtubestream.app.di.AppContainer
import com.youtubestream.app.ui.components.MiniPlayer
import com.youtubestream.app.ui.home.HomeScreen
import com.youtubestream.app.ui.imports.ImportScreen
import com.youtubestream.app.ui.library.LibraryScreen
import com.youtubestream.app.ui.player.PlayerScreen
import com.youtubestream.app.ui.search.SearchScreen
import com.youtubestream.app.ui.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Search("search", "Search", Icons.Filled.Search),
    Library("library", "Library", Icons.Filled.LibraryMusic),
    Player("player", "Player", Icons.Filled.PlayArrow),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    container: AppContainer,
    openPlayerSignal: Boolean = false,
    onPlayerOpened: () -> Unit = {},
) {
    val connection = container.playbackConnection
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination

    // Surface playback errors app-wide (this Scaffold is always composed), regardless of the open tab.
    val context = LocalContext.current
    LaunchedEffect(connection) {
        connection.messages.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    // Widget body-tap deep link: navigate to the Player once, then clear the signal.
    LaunchedEffect(openPlayerSignal) {
        if (openPlayerSignal) {
            nav.navigate(Dest.Player.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onPlayerOpened()
        }
    }

    Scaffold(
        topBar = {
            // Centralized app-shell title bar (D4): one TopAppBar, titled per tab.
            // Player stays immersive (D5) → no bar; "import" is a sub-route with its own back bar.
            val dest = Dest.entries.firstOrNull { d -> route?.hierarchy?.any { it.route == d.route } == true }
            if (dest != null && dest != Dest.Player) {
                TopAppBar(
                    title = { Text(dest.label) },
                    actions = {
                        if (dest == Dest.Library) {
                            IconButton(onClick = { nav.navigate("import") }) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = "Import from Pi")
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                MiniPlayer(
                    controller = connection,
                    onClick = {
                        nav.navigate(Dest.Player.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
                NavigationBar {
                    Dest.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = route?.hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                nav.navigate(dest.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Dest.Home.route, modifier = Modifier.padding(padding)) {
            composable(Dest.Home.route) { HomeScreen(Modifier.fillMaxSize()) }
            composable(Dest.Search.route) { SearchScreen(Modifier.fillMaxSize()) }
            composable(Dest.Settings.route) { SettingsScreen(Modifier.fillMaxSize()) }
            composable(Dest.Library.route) {
                LibraryScreen(modifier = Modifier.fillMaxSize())
            }
            composable("import") { ImportScreen(onBack = { nav.popBackStack() }, modifier = Modifier.fillMaxSize()) }
            composable(Dest.Player.route) {
                PlayerScreen(
                    connection = connection,
                    onBrowseLibrary = {
                        nav.navigate(Dest.Library.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
