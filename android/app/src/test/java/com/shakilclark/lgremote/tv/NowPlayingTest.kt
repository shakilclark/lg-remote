package com.shakilclark.lgremote.tv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure media-server foreground parsing (the subscription I/O is verified on a device). */
class NowPlayingTest {

    private fun json(s: String): JsonObject = Json.decodeFromString(JsonObject.serializer(), s)

    @Test
    fun `reads appId and play-state from the first foreground entry`() {
        val playing = json("""{"foregroundAppInfo":[{"appId":"netflix","playState":"playing","type":"media"}],"returnValue":true}""")
        assertEquals(MediaForeground("netflix", PlayState.Playing), parseMediaForeground(playing))

        val paused = json("""{"foregroundAppInfo":[{"appId":"youtube.leanback.v4","playState":"paused"}]}""")
        assertEquals(MediaForeground("youtube.leanback.v4", PlayState.Paused), parseMediaForeground(paused))
    }

    @Test
    fun `stopped and unloaded map to Stopped`() {
        assertEquals(PlayState.Stopped, parseMediaForeground(json("""{"foregroundAppInfo":[{"appId":"x","playState":"stopped"}]}""")).playState)
        assertEquals(PlayState.Stopped, parseMediaForeground(json("""{"foregroundAppInfo":[{"appId":"x","playState":"unloaded"}]}""")).playState)
    }

    @Test
    fun `empty or missing foreground info is unknown with no app`() {
        val empty = parseMediaForeground(json("""{"foregroundAppInfo":[],"returnValue":true}"""))
        assertNull(empty.appId)
        assertEquals(PlayState.Unknown, empty.playState)
        assertNull(parseMediaForeground(json("""{"returnValue":true}""")).appId)
    }

    @Test
    fun `unrecognised play-state is unknown`() {
        assertEquals(PlayState.Unknown, parseMediaForeground(json("""{"foregroundAppInfo":[{"appId":"x","playState":"buffering"}]}""")).playState)
    }

    @Test
    fun `system surfaces are excluded, content apps are not`() {
        // Home / inputs / settings / browser / live-TV → not "now playing".
        listOf("com.webos.app.home", "com.webos.app.hdmi1", "com.webos.app.livetv", "com.webos.app.browser", "com.palm.app.settings")
            .forEach { assertTrue(isSystemSurface(it), "expected system: $it") }
        // Content apps → shown.
        listOf("netflix", "youtube.leanback.v4", "com.disney.disneyplus").forEach {
            assertFalse(isSystemSurface(it), "expected content: $it")
        }
    }
}
