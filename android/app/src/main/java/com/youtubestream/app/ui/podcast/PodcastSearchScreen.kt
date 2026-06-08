package com.youtubestream.app.ui.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.ui.UiState
import com.youtubestream.app.ui.appViewModel

/** Focused full-screen podcast shows-search, launched from the Podcast tab's app-bar magnifier.
 *  Mirrors the music [com.youtubestream.app.ui.search.SearchScreen] shell; results are show cards
 *  that tap through to the existing ShowDetailScreen. No recents (kept separate from music recents). */
@Composable
fun PodcastSearchScreen(
    onBack: () -> Unit,
    onShowClick: (showId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = appViewModel { PodcastSearchViewModel(it.podcastRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // The page exists to type a query — open with the field focused and the keyboard up.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun submit(q: String) {
        query = q
        vm.search(q)
        focusManager.clearFocus()
    }

    Column(modifier = modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search shows") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(query) }),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close search")
            }
        }

        Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is UiState.Idle -> Text("Search for a show")
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${s.message}")
                    Button(onClick = { vm.search(query) }) { Text("Retry") }
                }
                is UiState.Content -> if (s.data.isEmpty()) {
                    Text("No shows found")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.data, key = { it.showId }) { show ->
                            ShowResultRow(show = show, onClick = { onShowClick(show.showId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowResultRow(show: PodcastShowCard, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = show.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(show.title, maxLines = 1)
            show.author?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 1) }
        }
    }
}
