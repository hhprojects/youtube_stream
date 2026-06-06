package com.youtubestream.app.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeUrlTest {

    @Test fun parsesStandardWatchUrl() {
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test fun parsesWatchUrlWithExtraParams() {
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("https://youtube.com/watch?v=dQw4w9WgXcQ&list=PL123&t=42s"))
    }

    @Test fun parsesMusicUrl() {
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ&si=abc"))
    }

    @Test fun parsesShortLink() {
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=xyz"))
    }

    @Test fun parsesShortsAndEmbed() {
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test fun acceptsBareId() {
        assertEquals("dQw4w9WgXcQ", YouTubeUrl.extractVideoId("  dQw4w9WgXcQ  "))
    }

    @Test fun rejectsNonYoutubeOrGarbage() {
        assertNull(YouTubeUrl.extractVideoId("https://example.com/watch?v=nope"))
        assertNull(YouTubeUrl.extractVideoId("just some text"))
        assertNull(YouTubeUrl.extractVideoId(""))
        assertNull(YouTubeUrl.extractVideoId("   "))
        assertNull(YouTubeUrl.extractVideoId("abc123"))   // too short to be an id
    }
}
