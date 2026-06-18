package com.shakilclark.lgremote.tv

import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.connection.TvConnectionManager
import com.shakilclark.lgremote.data.TvConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class CommandsTest {

    private lateinit var server: MockWebServer

    @AfterEach
    fun tearDown() {
        if (::server.isInitialized) runCatching { server.shutdown() }
    }

    // --- pure parseVolume (no network) ---

    @Test
    fun `parseVolume reads the flat shape`() {
        val v = parseVolume(json("""{"volume":13,"muted":true}"""))
        assertEquals(13, v.volume)
        assertEquals(true, v.muted)
    }

    @Test
    fun `parseVolume reads the nested volumeStatus shape`() {
        val v = parseVolume(json("""{"volumeStatus":{"volume":7,"muteStatus":false}}"""))
        assertEquals(7, v.volume)
        assertEquals(false, v.muted)
    }

    @Test
    fun `parseVolume tolerates missing fields`() {
        val v = parseVolume(json("""{"returnValue":true}"""))
        assertNull(v.volume)
        assertNull(v.muted)
    }

    // --- command URIs over MockWebServer ---

    @Test
    fun `commands send the correct ssap uris and payloads`() = runBlocking {
        val received = CopyOnWriteArrayList<String>()
        server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    received += text
                    val env = Ssap.parse(text)
                    when (env.type) {
                        "register" -> webSocket.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                        "request" -> webSocket.send("""{"type":"response","id":"${env.id}","payload":{}}""")
                    }
                }
            }),
        )
        server.start()
        val base = server.url("/").toString()
        val manager = TvConnectionManager(
            scope = CoroutineScope(Dispatchers.IO),
            clientFactory = { SsapClient(OkHttpClient()) },
            urlFor = { base },
        )
        manager.connect(TvConnection(id = "t", name = "t", address = "127.0.0.1"))
        withTimeout(3000) { manager.state.first { it is ConnectionState.Connected } }

        val commands = Commands(manager)
        commands.volumeUp()
        commands.setMute(true)
        commands.play()

        assertTrue(received.any { it.contains("ssap://audio/volumeUp") })
        assertTrue(received.any { it.contains("ssap://audio/setMute") && it.contains("\"mute\":true") })
        assertTrue(received.any { it.contains("ssap://media.controls/play") })
        manager.disconnect()
    }

    private fun json(s: String): JsonObject = Json.decodeFromString(JsonObject.serializer(), s)
}
