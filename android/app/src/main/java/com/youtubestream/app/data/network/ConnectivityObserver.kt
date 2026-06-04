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
 * Bridges Android's [ConnectivityManager] into a [Flow] of "does the device have a validated internet
 * connection right now". This is the free, instant signal — it flips on airplane mode / Wi-Fi loss with
 * no network call. It says nothing about whether the *Pi* is reachable (that needs a probe).
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
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }
        }
        // Seed the current value before the first callback arrives, so combine() has something immediately.
        trySend(
            cm.activeNetwork
                ?.let { cm.getNetworkCapabilities(it) }
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        )
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
