package com.youtubestream.app.ui.home

import com.youtubestream.app.data.local.LibrarySong

enum class ShelfId { RECENTLY_PLAYED, ON_REPEAT, MOST_PLAYED, REDISCOVER, RECENTLY_ADDED }

/** One horizontal row on the Home screen. */
data class Shelf(
    val id: ShelfId,
    val title: String,
    val songs: List<LibrarySong>,
)
