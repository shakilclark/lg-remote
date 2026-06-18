package com.shakilclark.lgremote.tv

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Contract tests for the from-scratch SSAP client (T010), using MockWebServer as a fake webOS
 * socket. Plain ws:// (no TLS) — the TrustManager is exercised only against the real TV.
 */
class SsapClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: SsapClient
    private val received = CopyOnWriteArrayList<String>()

    /** A scriptable fake TV: records what the client sends, replies via [onClientMessage]. */
    private fun startFakeTv(onClientMessage: (SsapEnvelope, WebSocket) -> Unit) {
        server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    received += text
                    onClientMessage(Ssap.parse(text), webSocket)
                }
            }),
        )
        server.start()
        client = SsapClient(OkHttpClient())
    }

    private fun url() = server.url("/").toString()

    @AfterEach
    fun tearDown() {
        if (::client.isInitialized) client.close()
        // MockWebServer.shutdown() can throw a benign "gave up waiting for queue" IOException
        // while a WebSocket is still winding down; teardown shouldn't fail the test over it.
        if (::server.isInitialized) runCatching { server.shutdown() }
    }

    @Test
    fun `register with stored key registers without a prompt`() = runBlocking {
        startFakeTv { env, ws ->
            if (env.type == "register") {
                ws.send("""{"type":"registered","payload":{"client-key":"KEY-123"}}""")
            }
        }
        client.connect(url(), clientKey = "KEY-123")
        val event = withTimeout(3000) { client.events.first { it is SsapEvent.Registered } }
        assertEquals("KEY-123", (event as SsapEvent.Registered).clientKey)
        // The outgoing register carried the stored key.
        assertTrue(received.any { it.contains("\"type\":\"register\"") && it.contains("KEY-123") })
    }

    @Test
    fun `register without key prompts then registers`() = runBlocking {
        startFakeTv { env, ws ->
            if (env.type == "register") {
                // First the on-screen prompt, then (user accepts) the key.
                ws.send("""{"type":"response","id":"${env.id}","payload":{"pairingType":"PROMPT","returnValue":true}}""")
                ws.send("""{"type":"registered","payload":{"client-key":"FRESH-KEY"}}""")
            }
        }
        client.connect(url(), clientKey = null)
        val prompt = withTimeout(3000) { client.events.first { it is SsapEvent.PromptPending } }
        assertTrue(prompt is SsapEvent.PromptPending)
        val reg = withTimeout(3000) { client.events.first { it is SsapEvent.Registered } }
        assertEquals("FRESH-KEY", (reg as SsapEvent.Registered).clientKey)
    }

    @Test
    fun `request returns the matching response payload`() = runBlocking {
        startFakeTv { env, ws ->
            when (env.type) {
                "register" -> ws.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                "request" -> ws.send("""{"type":"response","id":"${env.id}","payload":{"volume":7,"muted":false}}""")
            }
        }
        client.connect(url(), clientKey = "k")
        withTimeout(3000) { client.events.first { it is SsapEvent.Registered } }
        val payload: JsonObject = withTimeout(3000) { client.request("ssap://audio/getVolume") }
        assertEquals("7", payload["volume"].toString())
        // Outgoing request used the right uri + a request type.
        assertTrue(received.any { it.contains("\"type\":\"request\"") && it.contains("ssap://audio/getVolume") })
    }

    @Test
    fun `error response surfaces as an exception`() = runBlocking {
        startFakeTv { env, ws ->
            when (env.type) {
                "register" -> ws.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                "request" -> ws.send("""{"type":"error","id":"${env.id}","error":"401 insufficient permissions"}""")
            }
        }
        client.connect(url(), clientKey = "k")
        withTimeout(3000) { client.events.first { it is SsapEvent.Registered } }
        val ex = assertThrows<SsapException> {
            runBlocking { withTimeout(3000) { client.request("ssap://audio/volumeUp") } }
        }
        assertTrue(ex.message!!.contains("insufficient"))
    }

    @Test
    fun `subscription receives pushed payloads`() = runBlocking {
        lateinit var serverWs: WebSocket
        startFakeTv { env, ws ->
            serverWs = ws
            when (env.type) {
                "register" -> ws.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                "subscribe" -> ws.send("""{"type":"response","id":"${env.id}","payload":{"volume":3}}""")
            }
        }
        client.connect(url(), clientKey = "k")
        withTimeout(3000) { client.events.first { it is SsapEvent.Registered } }
        val flow = client.subscribe("ssap://audio/getVolume")
        val first = withTimeout(3000) { flow.first() }
        assertEquals("3", first["volume"].toString())
        assertNotNull(serverWs)
    }

    private fun sample(): JsonObject = buildJsonObject { put("mute", true) }
}
