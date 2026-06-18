package com.shakilclark.lgremote.tv

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Wire format for the webOS pointer-input socket (Connect-SDK WebOSTVMouseSocketConnection,
 * research R4). Newline-delimited fields, blank line terminates. Pure + unit-tested.
 */
object PointerFrames {
    fun button(name: String): String = "type:button\nname:$name\n\n"
    fun move(dx: Int, dy: Int, drag: Boolean = false): String =
        "type:move\ndx:$dx\ndy:$dy\ndown:${if (drag) 1 else 0}\n\n"
    fun click(): String = "type:click\n\n"
}

/**
 * The secondary WebSocket that carries D-pad buttons (US3) and the motion cursor (US5).
 * Obtain its url via `ssap://com.webos.service.networkinput/getPointerInputSocket`, then
 * [connect]. Reuses the TV trust client (the path is also a self-signed wss://).
 */
class PointerSocket(private val client: OkHttpClient = TvTrustManager.client()) {

    private var ws: WebSocket? = null
    val isOpen: Boolean get() = ws != null

    fun connect(socketPath: String) {
        val request = Request.Builder().url(socketPath.toPointerHttpUrl()).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ws = null
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ws = null
            }
        })
    }

    fun button(button: NavButton) { ws?.send(PointerFrames.button(button.name)) }
    fun move(dx: Int, dy: Int, drag: Boolean = false) { ws?.send(PointerFrames.move(dx, dy, drag)) }
    fun click() { ws?.send(PointerFrames.click()) }

    fun close() {
        ws?.close(1000, "client closing")
        ws = null
    }
}

private fun String.toPointerHttpUrl() = when {
    startsWith("wss://") -> "https://" + substring(6)
    startsWith("ws://") -> "http://" + substring(5)
    else -> this
}.toHttpUrl()
