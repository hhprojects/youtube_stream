package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.network.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverVisibilityTest {

    @Test fun reachableShows() {
        assertEquals(DiscoverVisibility.SHOW, discoverVisibility(ServerStatus.REACHABLE))
    }

    @Test fun checkingShowsSkeleton() {
        assertEquals(DiscoverVisibility.SKELETON, discoverVisibility(ServerStatus.CHECKING))
    }

    @Test fun offlineOrUnreachableHides() {
        assertEquals(DiscoverVisibility.HIDDEN, discoverVisibility(ServerStatus.DEVICE_OFFLINE))
        assertEquals(DiscoverVisibility.HIDDEN, discoverVisibility(ServerStatus.SERVER_UNREACHABLE))
    }
}
