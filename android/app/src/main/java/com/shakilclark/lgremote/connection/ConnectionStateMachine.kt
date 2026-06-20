package com.shakilclark.lgremote.connection

import com.shakilclark.lgremote.tv.SsapEvent
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pure transition logic (T011) — kept free of Android/coroutines so it's directly unit-testable
 * (T012). The orchestrator below feeds it [SsapEvent]s and reachability and owns the socket.
 */
object ConnectionReducer {

    fun reduce(current: ConnectionState, event: SsapEvent): ConnectionState = when (event) {
        SsapEvent.Open -> ConnectionState.Connecting
        SsapEvent.PromptPending -> ConnectionState.NeedsPairing()
        is SsapEvent.Registered ->
            // Preserve any volume/mute already known so a re-register doesn't blank the UI.
            (current as? ConnectionState.Connected) ?: ConnectionState.Connected()
        is SsapEvent.Closed -> ConnectionState.Disconnected(event.reason.ifBlank { "Disconnected" })
        is SsapEvent.Failure -> classifyFailure(event.error)
    }

    /**
     * Classify a connection failure. A bad or refused *address* is the TV's fault, not the
     * phone's — it reads as Disconnected with a "check the IP" hint, NOT OffNetwork (which wrongly
     * says "not on the same network" for a simple typo). Only a genuine no-route-to-host — the
     * phone can't reach that subnet at all — counts as OffNetwork here; the authoritative
     * off-network signal is the live [NetworkMonitor].
     */
    fun classifyFailure(error: Throwable?): ConnectionState = when (error) {
        is UnknownHostException -> ConnectionState.Disconnected("Can't find that TV — check the IP address")
        is ConnectException -> ConnectionState.Disconnected("Nothing's answering at that address — check the IP")
        is NoRouteToHostException -> ConnectionState.OffNetwork()
        is SocketTimeoutException -> ConnectionState.Disconnected("TV didn't respond")
        is IOException -> ConnectionState.Disconnected(error.message ?: "Can't reach your TV")
        null -> ConnectionState.Disconnected()
        else -> ConnectionState.Disconnected(error.message ?: "Can't reach your TV")
    }

    /** Commands are only accepted while connected (FR-014); never silently dropped. */
    fun acceptsCommands(state: ConnectionState): Boolean = state is ConnectionState.Connected
}
