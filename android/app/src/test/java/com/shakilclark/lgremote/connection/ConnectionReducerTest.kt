package com.shakilclark.lgremote.connection

import com.shakilclark.lgremote.tv.SsapEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Pure transition coverage (T012) — no network, no coroutines. */
class ConnectionReducerTest {

    @Test
    fun `open then prompt then registered reaches connected`() {
        var s: ConnectionState = ConnectionState.Disconnected()
        s = ConnectionReducer.reduce(s, SsapEvent.Open)
        assertEquals(ConnectionState.Connecting, s)
        s = ConnectionReducer.reduce(s, SsapEvent.PromptPending)
        assertTrue(s is ConnectionState.NeedsPairing)
        s = ConnectionReducer.reduce(s, SsapEvent.Registered("KEY"))
        assertTrue(s is ConnectionState.Connected)
    }

    @Test
    fun `registered preserves known volume and mute`() {
        val connected = ConnectionState.Connected(volume = 12, muted = true)
        val next = ConnectionReducer.reduce(connected, SsapEvent.Registered("KEY"))
        assertEquals(connected, next)
    }

    @Test
    fun `unreachable host maps to off-network`() {
        assertTrue(ConnectionReducer.classifyFailure(ConnectException("refused")) is ConnectionState.OffNetwork)
        assertTrue(ConnectionReducer.classifyFailure(UnknownHostException("no dns")) is ConnectionState.OffNetwork)
    }

    @Test
    fun `timeout maps to disconnected not off-network`() {
        val s = ConnectionReducer.classifyFailure(SocketTimeoutException("slow"))
        assertTrue(s is ConnectionState.Disconnected)
    }

    @Test
    fun `closed event maps to disconnected with reason`() {
        val s = ConnectionReducer.reduce(ConnectionState.Connected(), SsapEvent.Closed("tv asleep"))
        assertTrue(s is ConnectionState.Disconnected)
        assertEquals("tv asleep", (s as ConnectionState.Disconnected).message)
    }

    @Test
    fun `commands only accepted while connected`() {
        assertTrue(ConnectionReducer.acceptsCommands(ConnectionState.Connected()))
        assertFalse(ConnectionReducer.acceptsCommands(ConnectionState.Connecting))
        assertFalse(ConnectionReducer.acceptsCommands(ConnectionState.Disconnected()))
        assertFalse(ConnectionReducer.acceptsCommands(ConnectionState.NeedsPairing()))
        assertFalse(ConnectionReducer.acceptsCommands(ConnectionState.OffNetwork()))
    }
}
