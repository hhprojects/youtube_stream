package com.youtubestream.app.data.network

/** App-wide reachability of the Pi backend. The UI reads this to gate Pi-dependent features. */
enum class ServerStatus { CHECKING, REACHABLE, DEVICE_OFFLINE, SERVER_UNREACHABLE }

/** Outcome of the last touch of the Pi — set by a [ServerReachability.probe] or the interceptor's report. */
enum class ProbeResult { UNKNOWN, OK, FAILED }

/**
 * Pure reducer: (device connectivity, last Pi touch) → status. Zero Android imports, so it unit-tests
 * on the JVM — the fast base of the testing pyramid. Device-offline wins over a stale probe result.
 */
object ServerStatusReducer {
    fun reduce(deviceOnline: Boolean, probe: ProbeResult): ServerStatus = when {
        !deviceOnline               -> ServerStatus.DEVICE_OFFLINE
        probe == ProbeResult.OK     -> ServerStatus.REACHABLE
        probe == ProbeResult.FAILED -> ServerStatus.SERVER_UNREACHABLE
        else                        -> ServerStatus.CHECKING   // first probe in flight → optimistic, no banner
    }
}

/**
 * Pi actions are allowed unless we *know* the server is bad. CHECKING stays optimistic so a cold start
 * doesn't flash a disabled UI before the first probe returns.
 */
val ServerStatus.allowsPiActions: Boolean
    get() = this == ServerStatus.REACHABLE || this == ServerStatus.CHECKING
