package com.shakilclark.lgremote.tv

import com.shakilclark.lgremote.connection.TvConnectionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Volume/mute snapshot from the TV's getVolume subscription. */
data class VolumeState(val volume: Int? = null, val muted: Boolean? = null)

/**
 * Control commands over SSAP (US2; contracts/ssap-protocol.md). Stateless except the play/pause
 * toggle, which tracks the last media action (webOS has no reliable play-state query — R2/001).
 */
class Commands(private val manager: TvConnectionManager) {

    private var playing: Boolean? = null

    suspend fun volumeUp() { manager.request("ssap://audio/volumeUp") }
    suspend fun volumeDown() { manager.request("ssap://audio/volumeDown") }

    suspend fun setMute(mute: Boolean) {
        manager.request("ssap://audio/setMute", buildJsonObject { put("mute", mute) })
    }

    suspend fun play() { manager.request("ssap://media.controls/play"); playing = true }
    suspend fun pause() { manager.request("ssap://media.controls/pause"); playing = false }

    /** Toggle; default to pause when the state is unknown (matches the 001 behaviour). */
    suspend fun playPause() { if (playing == true) pause() else play() }

    /** Live volume/mute stream; empty when not connected. */
    fun volumeUpdates(): Flow<VolumeState> =
        (manager.subscribe("ssap://audio/getVolume") ?: emptyFlow()).map { parseVolume(it) }
}

/**
 * The TV returns volume either flat `{ volume, muted }` or nested
 * `{ volumeStatus: { volume, muteStatus } }` depending on firmware — handle both (observed on
 * the real TV in 001). Pure + unit-tested.
 */
fun parseVolume(payload: JsonObject): VolumeState {
    val flatVol = payload["volume"]?.jsonPrimitive?.intOrNull
    val flatMute = (payload["muted"] ?: payload["mute"])?.jsonPrimitive?.booleanOrNull
    val nested = payload["volumeStatus"] as? JsonObject
    val volume = flatVol ?: nested?.get("volume")?.jsonPrimitive?.intOrNull
    val muted = flatMute ?: nested?.get("muteStatus")?.jsonPrimitive?.booleanOrNull
    return VolumeState(volume, muted)
}
