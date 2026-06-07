package com.youtubestream.app.ui.playlist

/**
 * What a [PlaylistDetailScreen] is showing: a user's **manual** playlist (editable — drag, remove,
 * rename, delete, cover) or a derived **smart** playlist (read-only — just play). One screen, two
 * modes; this sealed type is the switch the screen and ViewModel branch on.
 */
sealed interface PlaylistSource {
    data class Manual(val id: Long) : PlaylistSource
    data class Smart(val kind: SmartKind) : PlaylistSource
}

/** The built-in smart playlists. [key] is the nav-arg token; [title] is the screen header. */
enum class SmartKind(val key: String, val title: String) {
    RECENTLY_PLAYED("recent", "Recently played"),
    MOST_PLAYED("most", "Most played"),
    ;

    companion object {
        /** Nav-arg key → kind. Unknown/garbage (e.g. a bad deep link) defaults to RECENTLY_PLAYED, never crashes. */
        fun fromKey(key: String?): SmartKind = entries.firstOrNull { it.key == key } ?: RECENTLY_PLAYED
    }
}
