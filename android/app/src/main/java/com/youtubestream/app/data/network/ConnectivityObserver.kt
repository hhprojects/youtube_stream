package com.youtubestream.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Bridges Android's [ConnectivityManager] into a [Flow] of "does the device have a network that provides
 * internet right now". This is the free, instant signal — it flips on airplane mode / Wi-Fi loss with no
 * network call. It says nothing about whether the *Pi* is reachable (that needs a probe).
 *
 * We check NET_CAPABILITY_INTERNET (the link *offers* internet), NOT NET_CAPABILITY_VALIDATED (Android
 * confirmed the *public* internet). The Pi runs on the LAN / over Tailscale, so a network without validated
 * public internet can still reach it — validating would wrongly report "offline". The probe is the real
 * arbiter of whether the *server* is reachable.
 *
 * Holds only the (application) [ConnectivityManager], never an Activity context — no leak.
 */
class ConnectivityObserver(context: Context) {

    private val cm = requireNotNull(context.getSystemService(ConnectivityManager::class.java))

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onUnavailable() { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        // Seed the current value before the first callback arrives, so combine() has something immediately.
        trySend(
            cm.activeNetwork
                ?.let { cm.getNetworkCapabilities(it) }
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        )
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
