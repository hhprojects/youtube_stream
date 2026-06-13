package com.youtubestream.app.ui.download

import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** One active (Queued/Downloading) download, for the global indicator/sheet. */
data class ActiveDownload(val key: String, val title: String, val status: ItemDownload)

/**
 * App-scoped sequential download queue. [enqueue] marks an id Queued and launches a coroutine that
 * waits on a FAIR [Mutex] (→ FIFO, one download at a time); a waiting id shows [ItemDownload.Queued]
 * until its turn. Because it runs on an app scope (not a viewModelScope), downloads survive leaving the
 * screen. Generic over the work flow so songs + episodes reuse it.
 *
 * [scope] should be Main-confined so the bookkeeping is single-threaded; the repo flows do their own
 * blocking IO via `flowOn(IO)`, so Main is never blocked.
 */
class DownloadQueue(private val scope: CoroutineScope) {
    private val _downloads = MutableStateFlow<Map<String, ItemDownload>>(emptyMap())
    /** id → Queued/Downloading/Failed. Absent = idle or finished. Song/episode rows read this. */
    val downloads: StateFlow<Map<String, ItemDownload>> = _downloads.asStateFlow()

    private val _active = MutableStateFlow<List<ActiveDownload>>(emptyList())
    /** Queued + Downloading items (with titles), for the global indicator. */
    val active: StateFlow<List<ActiveDownload>> = _active.asStateFlow()

    private val titles = ConcurrentHashMap<String, String>()
    private val jobs = ConcurrentHashMap<String, Job>()
    // The work thunk per key, retained so a Failed download can be re-run via [retry] (cleared on success/cancel).
    private val works = ConcurrentHashMap<String, () -> Flow<ItemDownload>>()
    private val mutex = Mutex()  // fair → first-enqueued-first-served

    private fun refreshActive() {
        _active.value = _downloads.value.entries
            // Failed is included so the indicator surfaces failures (with Retry), not just in-flight items.
            .filter { it.value is ItemDownload.Queued || it.value is ItemDownload.Downloading || it.value is ItemDownload.Failed }
            .map { ActiveDownload(it.key, titles[it.key] ?: it.key, it.value) }
    }

    private fun put(key: String, status: ItemDownload) {
        _downloads.update { it + (key to status) }
        refreshActive()
    }

    private fun remove(key: String) {
        titles.remove(key)
        works.remove(key)
        _downloads.update { it - key }
        refreshActive()
    }

    /**
     * Queue a download under [key] (labelled [title] for the indicator). Ignored if [key] is already
     * Queued/Downloading (dedupes double-taps). [work] is invoked only when it reaches the front.
     */
    fun enqueue(key: String, title: String, work: () -> Flow<ItemDownload>) {
        var accepted = false
        _downloads.update { m ->
            val ex = m[key]
            if (ex is ItemDownload.Queued || ex is ItemDownload.Downloading) m
            else { accepted = true; m + (key to ItemDownload.Queued) }
        }
        if (!accepted) return
        titles[key] = title
        works[key] = work
        refreshActive()
        jobs[key] = scope.launch {
            try {
                mutex.withLock {
                    put(key, ItemDownload.Downloading(0f))
                    var failed: ItemDownload.Failed? = null
                    work().collect { item ->
                        if (item is ItemDownload.Failed) failed = item
                        put(key, item)
                    }
                    val f = failed
                    if (f != null) put(key, f) else remove(key)   // keep Failed; remove on success
                }
            } finally {
                jobs.remove(key)
            }
        }
    }

    /** Cancel a queued or in-flight download. The repo flow deletes its partial file on cancel. */
    fun cancel(key: String) {
        jobs.remove(key)?.cancel()
        remove(key)
    }

    /** Re-run a previously-failed download using its retained work thunk. No-op if the key is unknown
     *  (e.g. called on the queue that doesn't own this key, mirroring [cancel]). */
    fun retry(key: String) {
        val work = works[key] ?: return
        enqueue(key, titles[key] ?: key, work)   // Failed isn't Queued/Downloading, so enqueue re-accepts it
    }
}
