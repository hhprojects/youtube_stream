package com.youtubestream.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {

    @Test fun trimsAndCollapsesInternalWhitespace() {
        assertEquals("lofi beats", normalizeQuery("  lofi   beats "))
    }

    @Test fun plainQueryIsUnchanged() {
        assertEquals("drake", normalizeQuery("drake"))
    }

    @Test fun blankOrWhitespaceReturnsNull() {
        assertNull(normalizeQuery(""))
        assertNull(normalizeQuery("   "))
        assertNull(normalizeQuery("\t\n"))
    }
}
