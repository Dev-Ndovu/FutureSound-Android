package com.futuresound.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.futuresound.player.FutureSoundApp
import com.futuresound.player.data.LibraryState
import com.futuresound.player.playback.PlaybackBridge
import com.futuresound.player.theme.FutureSoundTheme
import com.futuresound.player.ui.library.LibraryScreen
import com.futuresound.player.ui.nowplaying.NowPlayingScreen
import com.futuresound.player.ui.theme.ThemePickerScreen
import com.futuresound.player.visualizer.VisualizerEngine

private sealed class Destination(val route: String, val label: String) {
    object Library : Destination("library", "Library")
    object NowPlaying : Destination("now_playing", "Now Playing")
    object Themes : Destination("themes", "Themes")
}

/**
 * Root composable: hosts the bottom-nav graph and provides the active
 * FutureTheme + shared VisualizerEngine instance to every screen beneath it.
 */
@Composable
fun FutureSoundRoot(onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FutureSoundApp
    val repository = app.musicLibraryRepository

    val libraryState by repository.libraryState.collectAsStateWithLifecycle()
    val activeTheme by app.themeController.activeTheme.collectAsStateWithLifecycle()

    // One VisualizerEngine instance shared across recompositions/screens so
    // it doesn't reattach unnecessarily; released when the app process dies.
    val visualizerEngine = remember { VisualizerEngine() }

    FutureSoundTheme(theme = activeTheme) {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                AppBottomBar(navController)
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Library.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Destination.Library.route) {
                    when (val s = libraryState) {
                        is LibraryState.Loading -> CenteredMessage("Scanning local library...")
                        is LibraryState.NeedsPermission -> PermissionPrompt(onRequestPermission)
                        is LibraryState.Empty -> CenteredMessage(s.reason)
                        is LibraryState.Loaded -> {
                            val query by repository.searchQuery.collectAsStateWithLifecycle()
                            LibraryScreen(
                                songs = repository.filteredSongs(),
                                theme = activeTheme,
                                searchQuery = query,
                                onSearchQueryChange = { repository.setSearchQuery(it) },
                                onSongClick = { song ->
                                    val all = s.songs
                                    val index = all.indexOf(song).coerceAtLeast(0)
                                    PlaybackBridge.playQueue(all, index)
                                    navController.navigate(Destination.NowPlaying.route)
                                }
                            )
                        }
                    }
                }
                composable(Destination.NowPlaying.route) {
                    NowPlayingScreen(theme = activeTheme, visualizerEngine = visualizerEngine)
                }
                composable(Destination.Themes.route) {
                    ThemePickerScreen(
                        themes = app.themeController.allThemes(),
                        activeThemeId = activeTheme.id,
                        onThemeSelected = { id -> app.themeController.selectTheme(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavController) {
    val items = listOf(Destination.Library, Destination.NowPlaying, Destination.Themes)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    NavigationBar {
        items.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    val icon = when (destination) {
                        Destination.Library -> Icons.Filled.LibraryMusic
                        Destination.NowPlaying -> Icons.Filled.MusicNote
                        Destination.Themes -> Icons.Filled.Palette
                    }
                    Icon(icon, contentDescription = destination.label)
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FutureSound needs access to your music to play it offline.")
        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
            Text("Grant Access")
        }
    }
}
