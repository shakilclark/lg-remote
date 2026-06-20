package com.shakilclark.lgremote.connection

import com.shakilclark.lgremote.data.TvConnection
import com.shakilclark.lgremote.tv.SsapClient
import com.shakilclark.lgremote.tv.Ssap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration test for the manager + real SsapClient against a MockWebServer fake TV (T016):
 * connecting reaches Connected and the freshly-granted client-key is persisted.
 */
class TvConnectionManagerTest {

    private lateinit var server: MockWebServer

    @AfterEach
    fun tearDown() {
        if (::server.isInitialized) runCatching { server.shutdown() }
    }

    @Test
    fun `connect reaches Connected and persists the client-key`() = runBlocking {
        server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (Ssap.parse(text).type == "register") {
                        webSocket.send("""{"type":"registered","payload":{"client-key":"GRANTED-KEY"}}""")
                    }
                }
            }),
        )
        server.start()
        val base = server.url("/").toString()

        val persisted = AtomicReference<Pair<String, String>?>(null)
        val scope = CoroutineScope(Dispatchers.IO)
        val manager = TvConnectionManager(
            scope = scope,
            clientFactory = { SsapClient(OkHttpClient()) },
            persistClientKey = { id, key -> persisted.set(id to key) },
            urlFor = { base }, // point at the fake TV instead of wss://addr:3001
        )

        val tv = TvConnection(id = "tv-1", name = "Living Room", address = "127.0.0.1")
        manager.connect(tv)

        withTimeout(3000) { manager.state.first { it is ConnectionState.Connected } }
        // Persist callback fired with the new key for this TV.
        withTimeout(2000) {
            while (persisted.get() == null) kotlinx.coroutines.delay(20)
        }
        assertEquals("tv-1" to "GRANTED-KEY", persisted.get())
        assertTrue(manager.state.value is ConnectionState.Connected)
        manager.disconnect()
    }

    @Test
    fun `auto-reconnects after the socket drops (008 fast backoff)`() = runBlocking {
        server = MockWebServer()
        // First session: register, then the TV drops the socket.
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (Ssap.parse(text).type == "register") {
                        webSocket.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                        webSocket.close(1000, "drop")
                    }
                }
            }),
        )
        // Second session: register and stay — the reconnect should land here.
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (Ssap.parse(text).type == "register") {
                        webSocket.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                    }
                }
            }),
        )
        server.start()
        val base = server.url("/").toString()
        val scope = CoroutineScope(Dispatchers.IO)
        val manager = TvConnectionManager(
            scope = scope,
            clientFactory = { SsapClient(OkHttpClient()) },
            urlFor = { base },
        )
        val connects = java.util.concurrent.atomic.AtomicInteger(0)
        val watch = scope.launch {
            manager.state.collect { if (it is ConnectionState.Connected) connects.incrementAndGet() }
        }
        manager.connect(TvConnection(id = "tv", name = "tv", address = "127.0.0.1"))
        // Connected once, dropped, then reconnected → Connected a second time (fast backoff).
        withTimeout(8000) { while (connects.get() < 2) kotlinx.coroutines.delay(20) }
        assertTrue(connects.get() >= 2)
        watch.cancel()
        manager.disconnect()
    }
}
