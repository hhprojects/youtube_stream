package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryDownloadsTest {

    private fun song(id: String) = LibrarySong(id, "T", "A", 0, "$id.m4a", "/p/$id.m4a", 1L, 1L)

    @Test fun tracksProgressClearsAndPlaysOnComplete() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val channel = Channel<DownloadState>(Channel.UNLIMITED)
        var played: LibrarySong? = null
        val dl = DiscoveryDownloads(downloader = { _, _, _ -> channel.receiveAsFlow() }, play = { played = it })

        dl.download(scope, "v1", "Title", "art")
        scope.runCurrent()
        channel.send(DownloadState.InProgress(0.5f)); scope.runCurrent()
        assertEquals(ItemDownload.Downloading(0.5f), dl.downloads.value["v1"])

        val s = song("v1")
        channel.send(DownloadState.Completed(s)); scope.runCurrent()
        assertNull(dl.downloads.value["v1"])   // cleared
        assertEquals(s, played)                // and played
        channel.close()
    }

    @Test fun marksFailed() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val channel = Channel<DownloadState>(Channel.UNLIMITED)
        val dl = DiscoveryDownloads(downloader = { _, _, _ -> channel.receiveAsFlow() }, play = {})
        dl.download(scope, "v2", "T", null); scope.runCurrent()
        channel.send(DownloadState.Failed(RuntimeException("boom"))); scope.runCurrent()
        assertTrue(dl.downloads.value["v2"] is ItemDownload.Failed)
        channel.close()
    }

    @Test fun ignoresDoubleTap() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        var calls = 0
        val dl = DiscoveryDownloads(downloader = { _, _, _ -> calls++; kotlinx.coroutines.flow.emptyFlow() }, play = {})
        dl.download(scope, "v3", "T", null)
        dl.download(scope, "v3", "T", null)   // ignored while Downloading
        scope.runCurrent()
        assertEquals(1, calls)
    }
}
