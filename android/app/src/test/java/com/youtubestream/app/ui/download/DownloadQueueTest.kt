package com.youtubestream.app.ui.download

import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQueueTest {

    @Test fun `successful download ends up removed from the map`() = runTest(UnconfinedTestDispatcher()) {
        val q = DownloadQueue(backgroundScope)
        q.enqueue("a", "A") { flowOf(ItemDownload.Downloading(1f)) }
        advanceUntilIdle()
        assertEquals(null, q.downloads.value["a"])
    }

    @Test fun `a failed download stays Failed`() = runTest(UnconfinedTestDispatcher()) {
        val q = DownloadQueue(backgroundScope)
        q.enqueue("a", "A") { flowOf(ItemDownload.Downloading(0.3f), ItemDownload.Failed("boom")) }
        advanceUntilIdle()
        assertEquals(ItemDownload.Failed("boom"), q.downloads.value["a"])
    }

    @Test fun `second enqueue stays Queued while the first is active`() = runTest(UnconfinedTestDispatcher()) {
        val q = DownloadQueue(backgroundScope)
        // 'a' holds the queue open (never completes) so we can observe 'b' waiting.
        q.enqueue("a", "A") { flow { emit(ItemDownload.Downloading(0.1f)); awaitCancellation() } }
        q.enqueue("b", "B") { flowOf(ItemDownload.Downloading(1f)) }
        advanceUntilIdle()
        assertEquals(ItemDownload.Queued, q.downloads.value["b"])
        assertTrue(q.downloads.value["a"] is ItemDownload.Downloading)
    }

    @Test fun `enqueue of an in-progress key is ignored`() = runTest(UnconfinedTestDispatcher()) {
        val q = DownloadQueue(backgroundScope)
        var starts = 0
        val work = { starts++; flow<ItemDownload> { awaitCancellation() } }
        q.enqueue("a", "A", work)
        q.enqueue("a", "A", work)
        advanceUntilIdle()
        assertEquals(1, starts)
    }

    @Test fun `cancel removes an in-flight download`() = runTest(UnconfinedTestDispatcher()) {
        val q = DownloadQueue(backgroundScope)
        q.enqueue("a", "A") { flow { emit(ItemDownload.Downloading(0.1f)); awaitCancellation() } }
        advanceUntilIdle()
        assertTrue(q.downloads.value["a"] is ItemDownload.Downloading)
        q.cancel("a")
        advanceUntilIdle()
        assertEquals(null, q.downloads.value["a"])
    }
}
