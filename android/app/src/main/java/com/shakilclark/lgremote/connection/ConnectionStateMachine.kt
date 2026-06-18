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

    /** A connection failure is "off-network" if the host can't be routed/resolved at all. */
    fun classifyFailure(error: Throwable?): ConnectionState = when (error) {
        is UnknownHostException,
        is NoRouteToHostException,
        is ConnectException,
        -> ConnectionState.OffNetwork()
        is SocketTimeoutException -> ConnectionState.Disconnected("TV didn't respond")
        is IOException -> ConnectionState.Disconnected(error.message ?: "Can't reach your TV")
        null -> ConnectionState.Disconnected()
        else -> ConnectionState.Disconnected(error.message ?: "Can't reach your TV")
    }

    /** Commands are only accepted while connected (FR-014); never silently dropped. */
    fun acceptsCommands(state: ConnectionState): Boolean = state is ConnectionState.Connected
}
