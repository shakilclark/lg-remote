package com.shakilclark.lgremote.tv

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class PointerSocketTest {

    private lateinit var server: MockWebServer

    @AfterEach
    fun tearDown() {
        if (::server.isInitialized) runCatching { server.shutdown() }
    }

    // --- pure frame format (no network) ---

    @Test
    fun `button frame matches the webOS wire format`() {
        assertEquals("type:button\nname:UP\n\n", PointerFrames.button("UP"))
        assertEquals("type:button\nname:ENTER\n\n", PointerFrames.button("ENTER"))
    }

    @Test
    fun `move and click frames match the wire format`() {
        assertEquals("type:move\ndx:12\ndy:-8\ndown:0\n\n", PointerFrames.move(12, -8))
        assertEquals("type:move\ndx:1\ndy:2\ndown:1\n\n", PointerFrames.move(1, 2, drag = true))
        assertEquals("type:click\n\n", PointerFrames.click())
    }

    // --- socket sends frames over the wire ---

    @Test
    fun `connect opens the socket and sends button frames`() = runBlocking {
        val received = CopyOnWriteArrayList<String>()
        server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) { received += text }
            }),
        )
        server.start()

        val pointer = PointerSocket(OkHttpClient())
        pointer.connect(server.url("/pointer").toString())
        // Allow the upgrade to complete.
        var tries = 0
        while (!pointer.isOpen && tries++ < 100) kotlinx.coroutines.delay(10)
        assertTrue(pointer.isOpen)

        pointer.button(NavButton.LEFT)
        pointer.click()

        var seen = 0
        while (received.size < 2 && seen++ < 100) kotlinx.coroutines.delay(10)
        assertTrue(received.any { it == "type:button\nname:LEFT\n\n" })
        assertTrue(received.any { it == "type:click\n\n" })
        pointer.close()
    }
}
