package com.shakilclark.lgremote.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Emits whether the phone is currently on a network that could reach a LAN TV (Wi-Fi or
 * ethernet). Mobile-data / no-network → false, which the app surfaces as OffNetwork
 * (FR-009 edge case). Uses the default-network callback so it tracks transport changes live.
 */
class NetworkMonitor(context: Context) {

    private val cm = context.getSystemService(ConnectivityManager::class.java)

    val onLocalNetwork: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(isLocalTransport(caps))
            }
            override fun onLost(network: Network) { trySend(false) }
            override fun onUnavailable() { trySend(false) }
        }
        // Seed with the current state before changes arrive.
        val current = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        trySend(current != null && isLocalTransport(current))
        cm?.registerDefaultNetworkCallback(callback)
        awaitClose { cm?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

/** A network can reach the LAN TV only over Wi-Fi or ethernet (not cellular). Pure + testable. */
fun isLocalTransport(caps: NetworkCapabilities): Boolean =
    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
