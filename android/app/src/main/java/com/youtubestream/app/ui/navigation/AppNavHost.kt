package com.youtubestream.app.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.youtubestream.app.di.AppContainer
import com.youtubestream.app.ui.discover.DiscoveryListScreen
import com.youtubestream.app.ui.home.HomeScreen
import com.youtubestream.app.ui.imports.ImportScreen
import com.youtubestream.app.ui.library.LibraryHomeScreen
import com.youtubestream.app.ui.library.LibraryScreen
import com.youtubestream.app.ui.player.PlayerSheet
import com.youtubestream.app.ui.player.rememberPlayerSheetState
import com.youtubestream.app.ui.playlist.PlaylistDetailScreen
import com.youtubestream.app.ui.playlist.PlaylistSource
import com.youtubestream.app.ui.playlist.SmartKind
import com.youtubestream.app.ui.search.SearchScreen
import com.youtubestream.app.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Search("search", "Search", Icons.Filled.Search),
    Library("library", "Library", Icons.Filled.LibraryMusic),
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

    val sheet = rememberPlayerSheetState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val peekHeight = 62.dp                                   // art 44 + 16 vpad + 2dp line
    val peekHeightPx = with(density) { peekHeight.roundToPx() }
    var navBarHeightPx by remember { mutableIntStateOf(with(density) { 80.dp.roundToPx() }) }

    // The docked mini-bar shows when a track is loaded. Reserve bottom space for it so it doesn't
    // float over page content (e.g. the Import "Download" button). Recomposes only when this flips.
    val miniBarVisible by remember(connection) {
        connection.state.map { it.isConnected && it.currentMediaId != null }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    // Surface playback errors app-wide (this Scaffold is always composed), regardless of the open tab.
    val context = LocalContext.current
    LaunchedEffect(connection) {
        connection.messages.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    // Widget body-tap deep link: expand the player sheet once, then clear the signal.
    LaunchedEffect(openPlayerSignal) {
        if (openPlayerSignal) {
            sheet.expand()
            onPlayerOpened()
        }
    }

    // System/predictive back collapses the expanded player instead of leaving the app.
    BackHandler(enabled = sheet.isExpanded) { scope.launch { sheet.collapse() } }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // Centralized app-shell title bar: one TopAppBar, titled per tab.
                // "import" is a sub-route with its own back bar (not in Dest) → no bar.
                val dest = Dest.entries.firstOrNull { d -> route?.hierarchy?.any { it.route == d.route } == true }
                if (dest != null) {
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
                // Slides down off-screen as the sheet expands (progress 0→1).
                NavigationBar(
                    Modifier
                        .onSizeChanged { navBarHeightPx = it.height }
                        .graphicsLayer { translationY = sheet.progress * navBarHeightPx },
                ) {
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
            },
        ) { padding ->
            NavHost(nav, startDestination = Dest.Home.route, modifier = Modifier.padding(padding).padding(bottom = if (miniBarVisible) peekHeight else 0.dp)) {
                composable(Dest.Home.route) {
                    HomeScreen(
                        onOpenMood = { key -> nav.navigate("mood?key=" + android.net.Uri.encode(key)) },
                        onOpenGenre = { id, title ->
                            nav.navigate("genre?id=" + android.net.Uri.encode(id) + "&title=" + android.net.Uri.encode(title))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable(Dest.Search.route) { SearchScreen(Modifier.fillMaxSize()) }
                composable(Dest.Settings.route) { SettingsScreen(Modifier.fillMaxSize()) }
                composable(Dest.Library.route) {
                    LibraryHomeScreen(
                        onOpenAllSongs = { nav.navigate("allSongs") },
                        onOpenPlaylist = { id -> nav.navigate("playlist/$id") },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable("allSongs") {
                    LibraryScreen(onBack = { nav.popBackStack() }, modifier = Modifier.fillMaxSize())
                }
                composable(
                    "playlist/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    PlaylistDetailScreen(
                        source = PlaylistSource.Manual(entry.arguments?.getLong("id") ?: 0L),
                        onBack = { nav.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable(
                    "smart/{key}",
                    arguments = listOf(navArgument("key") { type = NavType.StringType; defaultValue = "" }),
                ) { entry ->
                    // SmartKind.fromKey defaults unknown/garbage keys to RECENTLY_PLAYED (no crash on a bad deep link).
                    PlaylistDetailScreen(
                        source = PlaylistSource.Smart(SmartKind.fromKey(entry.arguments?.getString("key"))),
                        onBack = { nav.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable("import") { ImportScreen(onBack = { nav.popBackStack() }, modifier = Modifier.fillMaxSize()) }
                composable(
                    "mood?key={key}",
                    arguments = listOf(navArgument("key") { type = NavType.StringType; defaultValue = "" }),
                ) { entry ->
                    val key = entry.arguments?.getString("key").orEmpty()
                    DiscoveryListScreen(
                        load = { it.moodSongs(key) },
                        fallbackTitle = "Mood",
                        onBack = { nav.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable(
                    "genre?id={id}&title={title}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType; defaultValue = "" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    DiscoveryListScreen(
                        load = { it.playlistSongs(id) },
                        fallbackTitle = "Genre",
                        titleOverride = entry.arguments?.getString("title")?.takeIf { it.isNotBlank() },
                        onBack = { nav.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        PlayerSheet(
            connection = connection,
            sheet = sheet,
            peekHeightPx = peekHeightPx,
            navBarHeightPx = navBarHeightPx,
        )
    }
}
