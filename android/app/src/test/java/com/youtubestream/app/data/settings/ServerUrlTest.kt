package com.youtubestream.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlTest {

    @Test fun prependsHttpWhenSchemeMissing() {
        assertEquals("http://192.168.1.5:3001", normalizeServerUrl("192.168.1.5:3001"))
    }

    @Test fun keepsValidHttpUrlUncanonicalized() {
        // No trailing slash added — the saved value matches what the user typed.
        assertEquals("http://pi:3001", normalizeServerUrl("http://pi:3001"))
    }

    @Test fun keepsHttpsScheme() {
        assertEquals("https://pi.local", normalizeServerUrl("https://pi.local"))
    }

    @Test fun trimsSurroundingWhitespace() {
        assertEquals("http://pi:3001", normalizeServerUrl("  http://pi:3001  "))
    }

    @Test fun blankIsNull() {
        assertNull(normalizeServerUrl(""))
        assertNull(normalizeServerUrl("   "))
    }

    @Test fun malformedIsNull() {
        assertNull(normalizeServerUrl("http://"))   // scheme but no host
    }
}
