package com.youtubestream.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youtubestream.app.di.AppContainer
import com.youtubestream.app.ui.components.MiniPlayer
import com.youtubestream.app.ui.debug.DebugPlaybackScreen
import com.youtubestream.app.ui.library.LibraryScreen
import com.youtubestream.app.ui.search.SearchScreen
import com.youtubestream.app.ui.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Search("search", "Search", Icons.Filled.Search),
    Library("library", "Library", Icons.Filled.LibraryMusic),
    Player("player", "Player", Icons.Filled.PlayArrow),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun AppNavHost(container: AppContainer) {
    val connection = container.playbackConnection
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination

    Scaffold(
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
        NavHost(nav, startDestination = Dest.Search.route, modifier = Modifier.padding(padding)) {
            composable(Dest.Search.route) { SearchScreen(Modifier.fillMaxSize()) }
            composable(Dest.Settings.route) { SettingsScreen(Modifier.fillMaxSize()) }
            composable(Dest.Library.route) { LibraryScreen(Modifier.fillMaxSize()) }
            composable(Dest.Player.route) {
                DebugPlaybackScreen(connection = connection, container = container, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
