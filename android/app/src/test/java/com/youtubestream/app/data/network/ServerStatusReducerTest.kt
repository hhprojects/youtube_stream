package com.youtubestream.app.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerStatusReducerTest {

    @Test fun deviceOffline_overridesAnyProbeResult() {
        assertEquals(ServerStatus.DEVICE_OFFLINE, ServerStatusReducer.reduce(false, ProbeResult.OK))
        assertEquals(ServerStatus.DEVICE_OFFLINE, ServerStatusReducer.reduce(false, ProbeResult.FAILED))
        assertEquals(ServerStatus.DEVICE_OFFLINE, ServerStatusReducer.reduce(false, ProbeResult.UNKNOWN))
    }

    @Test fun online_withOkProbe_isReachable() {
        assertEquals(ServerStatus.REACHABLE, ServerStatusReducer.reduce(true, ProbeResult.OK))
    }

    @Test fun online_withFailedProbe_isServerUnreachable() {
        assertEquals(ServerStatus.SERVER_UNREACHABLE, ServerStatusReducer.reduce(true, ProbeResult.FAILED))
    }

    @Test fun online_withNoProbeYet_isChecking() {
        assertEquals(ServerStatus.CHECKING, ServerStatusReducer.reduce(true, ProbeResult.UNKNOWN))
    }

    @Test fun allowsPiActions_isTrueOnlyForReachableAndChecking() {
        assertTrue(ServerStatus.REACHABLE.allowsPiActions)
        assertTrue(ServerStatus.CHECKING.allowsPiActions)
        assertFalse(ServerStatus.DEVICE_OFFLINE.allowsPiActions)
        assertFalse(ServerStatus.SERVER_UNREACHABLE.allowsPiActions)
    }
}
