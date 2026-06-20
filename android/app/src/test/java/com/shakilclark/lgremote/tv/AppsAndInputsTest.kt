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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class AppsAndInputsTest {

    private lateinit var server: MockWebServer

    @AfterEach
    fun tearDown() {
        if (::server.isInitialized) runCatching { server.shutdown() }
    }

    // --- pure resolution / parsing (no network) ---

    @Test
    fun `resolveLaunchPointIdByTitle matches title case-insensitively`() {
        val payload = json("""{"launchPoints":[{"title":"youtube","id":"youtube.leanback.v4"},{"title":"Netflix","id":"netflix"},{"title":"Settings","id":"com.palm.app.settings"}]}""")
        assertEquals("youtube.leanback.v4", resolveLaunchPointIdByTitle(payload, "YouTube"))
        assertEquals("netflix", resolveLaunchPointIdByTitle(payload, "Netflix"))
        assertEquals("com.palm.app.settings", resolveLaunchPointIdByTitle(payload, "Settings"))
    }

    @Test
    fun `resolveLaunchPointIdByTitle returns null when absent so caller falls back`() {
        val payload = json("""{"launchPoints":[{"title":"Disney+","id":"com.disney"}]}""")
        assertNull(resolveLaunchPointIdByTitle(payload, "YouTube"))
        assertNull(resolveLaunchPointIdByTitle(payload, "Settings"))
    }

    @Test
    fun `parseLaunchPoints reads id, title, icon in TV order, blank icon becomes null`() {
        val payload = json(
            """{"launchPoints":[
                {"id":"youtube.leanback.v4","title":"YouTube","icon":"https://tv:3001/r/yt.png"},
                {"id":"netflix","title":"Netflix","icon":""},
                {"id":"com.x"}
            ]}""",
        )
        val apps = parseLaunchPoints(payload)
        assertEquals(3, apps.size)
        assertEquals(TvApp("youtube.leanback.v4", "YouTube", "https://tv:3001/r/yt.png"), apps[0])
        assertNull(apps[1].iconUrl) // blank icon → null (UI falls back to a tile)
        assertEquals("com.x", apps[2].title) // missing title falls back to id
        assertTrue(parseLaunchPoints(json("""{"returnValue":true}""")).isEmpty())
    }

    @Test
    fun `parseInputs reads id label and connected`() {
        val payload = json("""{"devices":[{"id":"HDMI_1","label":"HDMI 1","connected":false},{"id":"HDMI_2","label":"PS4 Game Console","connected":true}]}""")
        val inputs = parseInputs(payload)
        assertEquals(2, inputs.size)
        assertEquals(TvInput("HDMI_2", "PS4 Game Console", connected = true), inputs[1])
    }

    @Test
    fun `parseInputs falls back label to id and tolerates empty`() {
        assertEquals(listOf(TvInput("HDMI_3", "HDMI_3")), parseInputs(json("""{"devices":[{"id":"HDMI_3"}]}""")))
        assertTrue(parseInputs(json("""{"returnValue":true}""")).isEmpty())
    }

    // --- launch + input over MockWebServer ---

    @Test
    fun `launchAppId sends launch and reports the TV result`() = runBlocking {
        val received = CopyOnWriteArrayList<String>()
        val commands = connectCommands(received) { env, ws ->
            if (env.uri == "ssap://system.launcher/launch") {
                val ok = env.payload.toString().contains("youtube.leanback.v4")
                ws.send("""{"type":"response","id":"${env.id}","payload":{"returnValue":$ok}}""")
            }
        }
        assertTrue(commands.launchAppId("youtube.leanback.v4"))
        assertTrue(received.any { it.contains("ssap://system.launcher/launch") && it.contains("youtube.leanback.v4") })
        assertFalse(commands.launchAppId("com.unknown")) // TV reports returnValue:false
    }

    @Test
    fun `listApps parses launch points from the TV`() = runBlocking {
        val commands = connectCommands(CopyOnWriteArrayList()) { env, ws ->
            if (env.uri == "ssap://com.webos.applicationManager/listLaunchPoints") {
                ws.send("""{"type":"response","id":"${env.id}","payload":{"launchPoints":[{"id":"netflix","title":"Netflix","icon":"https://tv:3001/r/n.png"}]}}""")
            }
        }
        val apps = commands.listApps()
        assertEquals(TvApp("netflix", "Netflix", "https://tv:3001/r/n.png"), apps.single())
    }

    @Test
    fun `listInputs and setInput hit the right uris`() = runBlocking {
        val received = CopyOnWriteArrayList<String>()
        val commands = connectCommands(received) { env, ws ->
            when (env.uri) {
                "ssap://tv/getExternalInputList" ->
                    ws.send("""{"type":"response","id":"${env.id}","payload":{"devices":[{"id":"HDMI_2","label":"PS4 Game Console","connected":true}]}}""")
                "ssap://tv/switchInput" -> ws.send("""{"type":"response","id":"${env.id}","payload":{"returnValue":true}}""")
            }
        }
        val inputs = commands.listInputs()
        assertEquals("PS4 Game Console", inputs.single().label)
        commands.setInput("HDMI_2")
        assertTrue(received.any { it.contains("ssap://tv/switchInput") && it.contains("\"inputId\":\"HDMI_2\"") })
    }

    /** Boot a manager+client connected to a fake TV, returning a ready Commands. */
    private suspend fun connectCommands(
        received: CopyOnWriteArrayList<String>,
        onRequest: (SsapEnvelope, WebSocket) -> Unit,
    ): Commands {
        server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    received += text
                    val env = Ssap.parse(text)
                    if (env.type == "register") {
                        webSocket.send("""{"type":"registered","payload":{"client-key":"k"}}""")
                    } else {
                        onRequest(env, webSocket)
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
        return Commands(manager)
    }

    private fun json(s: String): JsonObject = Json.decodeFromString(JsonObject.serializer(), s)
}
