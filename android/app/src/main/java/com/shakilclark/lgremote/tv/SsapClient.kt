package com.shakilclark.lgremote.tv

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Lifecycle of the SSAP socket, surfaced to the connection state machine. */
sealed interface SsapEvent {
    data object Open : SsapEvent
    /** Pairing succeeded (or was already valid); persist [clientKey]. */
    data class Registered(val clientKey: String?) : SsapEvent
    /** TV is showing the on-screen accept prompt; waiting for the user. */
    data object PromptPending : SsapEvent
    data class Closed(val reason: String) : SsapEvent
    data class Failure(val error: Throwable?) : SsapEvent
}

class SsapException(message: String) : Exception(message)

/**
 * Hand-written SSAP client over a single OkHttp WebSocket (research R2). Correlates responses
 * to requests by id, supports subscriptions, and drives the register/pairing handshake (R3).
 * Transport-agnostic: pass any [client] + ws [url] (prod uses [TvTrustManager]; tests use
 * MockWebServer).
 */
class SsapClient(private val client: OkHttpClient = TvTrustManager.client()) {

    private val ids = AtomicInteger(0)
    private var socket: WebSocket? = null
    private var registerId: String? = null

    private val pending = ConcurrentHashMap<String, CompletableDeferred<SsapEnvelope>>()
    private val subscriptions = ConcurrentHashMap<String, MutableSharedFlow<JsonObject>>()

    private val _events = MutableSharedFlow<SsapEvent>(replay = 1, extraBufferCapacity = 16)
    val events: Flow<SsapEvent> = _events.asSharedFlow()

    val isConnected: Boolean get() = socket != null

    /** Open the socket and begin the register handshake with an optional stored [clientKey]. */
    fun connect(url: String, clientKey: String?) {
        val request = Request.Builder().url(url.toHttpWsUrl()).build()
        socket = client.newWebSocket(request, Listener(clientKey))
    }

    fun close() {
        socket?.close(1000, "client closing")
        socket = null
        registerId = null
        pending.values.forEach { it.completeExceptionally(SsapException("socket closed")) }
        pending.clear()
        subscriptions.clear()
    }

    /** Send a one-shot request and await its response payload. */
    suspend fun request(uri: String, payload: JsonObject? = null): JsonObject {
        val ws = socket ?: throw SsapException("not connected")
        val id = nextId()
        val deferred = CompletableDeferred<SsapEnvelope>()
        pending[id] = deferred
        ws.send(Ssap.request(id, uri, payload))
        val env = deferred.await()
        if (env.type == "error") throw SsapException(env.error ?: "ssap error")
        return env.payload ?: JsonObject(emptyMap())
    }

    /** Subscribe to a uri; emits each payload the TV pushes (e.g. volume/mute). */
    fun subscribe(uri: String): Flow<JsonObject> {
        val ws = socket ?: throw SsapException("not connected")
        val id = nextId()
        val flow = MutableSharedFlow<JsonObject>(replay = 1, extraBufferCapacity = 16)
        subscriptions[id] = flow
        ws.send(Ssap.request(id, uri, subscribe = true))
        return flow.asSharedFlow()
    }

    private fun nextId(): String = "req_${ids.incrementAndGet()}"

    private inner class Listener(private val clientKey: String?) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _events.tryEmit(SsapEvent.Open)
            val id = nextId().also { registerId = it }
            webSocket.send(Ssap.register(id, clientKey))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val env = runCatching { Ssap.parse(text) }.getOrNull() ?: return
            // Registration handshake.
            if (env.type == "registered") {
                val key = env.payload?.get("client-key")?.toStringValue()
                _events.tryEmit(SsapEvent.Registered(key))
                return
            }
            if (env.id != null && env.id == registerId) {
                val pairing = env.payload?.get("pairingType")?.toStringValue()
                if (pairing == "PROMPT") _events.tryEmit(SsapEvent.PromptPending)
                return
            }
            // Subscriptions stay open and receive repeated responses.
            env.id?.let { id ->
                subscriptions[id]?.let { flow ->
                    env.payload?.let { flow.tryEmit(it) }
                    return
                }
                pending.remove(id)?.complete(env)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _events.tryEmit(SsapEvent.Closed(reason.ifEmpty { "closed" }))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            socket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            socket = null
            _events.tryEmit(SsapEvent.Failure(t))
        }
    }
}

/** Accept `wss://`/`ws://` urls by mapping to the http(s) scheme OkHttp's Request.url expects. */
private fun String.toHttpWsUrl(): okhttp3.HttpUrl {
    val mapped = when {
        startsWith("wss://") -> "https://" + substring(6)
        startsWith("ws://") -> "http://" + substring(5)
        else -> this
    }
    return mapped.toHttpUrl()
}

private fun kotlinx.serialization.json.JsonElement.toStringValue(): String? =
    (this as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else content.ifEmpty { null }
