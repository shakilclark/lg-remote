package com.shakilclark.lgremote.connection

import com.shakilclark.lgremote.data.TvConnection
import com.shakilclark.lgremote.tv.SsapClient
import com.shakilclark.lgremote.tv.SsapEvent
import com.shakilclark.lgremote.tv.TvTrustManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Owns the single SSAP socket to the active TV, turns [SsapEvent]s into [ConnectionState] via
 * [ConnectionReducer], persists a freshly-granted client-key, and auto-reconnects on drop
 * (FR-010). UI/command layers observe [state] and go through [request].
 */
class TvConnectionManager(
    private val scope: CoroutineScope,
    private val clientFactory: () -> SsapClient = { SsapClient(TvTrustManager.client()) },
    private val persistClientKey: suspend (tvId: String, clientKey: String) -> Unit = { _, _ -> },
    private val urlFor: (TvConnection) -> String = { TvTrustManager.wssUrl(it.address) },
    private val reconnectDelayMs: Long = 5_000,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var client: SsapClient? = null
    private var target: TvConnection? = null
    private var eventsJob: Job? = null
    private var wantConnection = false

    /** The live socket for command/pointer layers; null until connected. */
    val ssap: SsapClient? get() = client

    fun connect(tv: TvConnection) {
        wantConnection = true
        target = tv
        openSocket(tv)
    }

    private fun openSocket(tv: TvConnection) {
        eventsJob?.cancel()
        client?.close()
        _state.value = ConnectionState.Connecting
        val c = clientFactory().also { client = it }
        eventsJob = scope.launch {
            c.events.collect { event ->
                _state.value = ConnectionReducer.reduce(_state.value, event)
                if (event is SsapEvent.Registered && event.clientKey != null) {
                    persistClientKey(tv.id, event.clientKey)
                }
                if (event is SsapEvent.Closed || event is SsapEvent.Failure) {
                    scheduleReconnect()
                }
            }
        }
        c.connect(urlFor(tv), tv.clientKey)
    }

    private fun scheduleReconnect() {
        if (!wantConnection) return
        val tv = target ?: return
        scope.launch {
            delay(reconnectDelayMs)
            if (wantConnection && isActive && _state.value !is ConnectionState.Connected) {
                openSocket(tv)
            }
        }
    }

    fun disconnect() {
        wantConnection = false
        eventsJob?.cancel()
        client?.close()
        client = null
        _state.value = ConnectionState.Disconnected()
    }

    /** Send a command, rejecting it visibly when not connected (FR-014). */
    suspend fun request(uri: String, payload: JsonObject? = null): JsonObject {
        check(ConnectionReducer.acceptsCommands(_state.value)) { "TV not connected" }
        val c = client ?: error("TV not connected")
        return c.request(uri, payload)
    }
}
