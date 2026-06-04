package com.youtubestream.app.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What a ViewModel needs from reachability: observe [status] and trigger a [probe] (screen entry, retry). */
interface ReachabilitySource {
    val status: StateFlow<ServerStatus>
    suspend fun probe()
}

/**
 * The single source of truth for "can we use Pi features right now". Merges device connectivity with the
 * result of the last Pi touch (a [probe] call, or a real request via the interceptor's [report]). The merge
 * itself is the pure [ServerStatusReducer]; this class only holds the mutable [probeResult] state.
 */
class ServerReachability(
    connectivity: ConnectivityObserver,
    scope: CoroutineScope,
    private val probeAction: suspend () -> Unit,
) : ReachabilitySource {

    private val probeResult = MutableStateFlow(ProbeResult.UNKNOWN)

    override val status: StateFlow<ServerStatus> =
        combine(connectivity.isOnline, probeResult) { online, p ->
            ServerStatusReducer.reduce(online, p)
        }.stateIn(scope, SharingStarted.Eagerly, ServerStatus.CHECKING)

    /** Cheap, synchronous — the interceptor calls this on every fast-client request. */
    fun report(ok: Boolean) {
        probeResult.value = if (ok) ProbeResult.OK else ProbeResult.FAILED
    }

    /** Proactive check (app start, screen entry, manual Retry). [probeAction] reuses GET /api/library. */
    override suspend fun probe() = try {
        probeAction()
        report(true)
    } catch (e: Exception) {
        report(false)
    }
}
