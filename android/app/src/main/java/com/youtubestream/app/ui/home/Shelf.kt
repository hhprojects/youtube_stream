package com.youtubestream.app.ui.home

import com.youtubestream.app.data.local.LibrarySong

enum class ShelfId { RECENTLY_PLAYED, ON_REPEAT }

/** One horizontal row on the Home screen. */
data class Shelf(
    val id: ShelfId,
    val title: String,
    val songs: List<LibrarySong>,
)
