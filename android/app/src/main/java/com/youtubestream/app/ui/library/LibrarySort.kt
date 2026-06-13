package com.youtubestream.app.ui.library

import com.youtubestream.app.data.local.LibrarySong

/** Sort options for the All Songs list. [label] is shown in the sort menu. */
enum class LibrarySort(val label: String) {
    RECENTLY_ADDED("Recently added"),
    OLDEST("Oldest first"),
    TITLE("Title (A–Z)"),
    ARTIST("Artist (A–Z)"),
}

/** Pure: order a song list by [sort]. Title/Artist are case-insensitive; date sorts use [LibrarySong.dateAdded]. */
fun sortLibrary(songs: List<LibrarySong>, sort: LibrarySort): List<LibrarySong> = when (sort) {
    LibrarySort.RECENTLY_ADDED -> songs.sortedByDescending { it.dateAdded }
    LibrarySort.OLDEST -> songs.sortedBy { it.dateAdded }
    LibrarySort.TITLE -> songs.sortedBy { it.title.lowercase() }
    LibrarySort.ARTIST -> songs.sortedBy { it.artist.lowercase() }
}

/** Pure: keep songs whose title or artist contains [query] (case-insensitive). Blank query → unchanged. */
fun filterLibrary(songs: List<LibrarySong>, query: String): List<LibrarySong> {
    val q = query.trim()
    if (q.isEmpty()) return songs
    return songs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
}
