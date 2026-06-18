package com.shakilclark.lgremote.tv

import com.shakilclark.lgremote.connection.TvConnectionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Volume/mute snapshot from the TV's getVolume subscription. */
data class VolumeState(val volume: Int? = null, val muted: Boolean? = null)

/** An external input/source (US7). */
data class TvInput(val id: String, val label: String, val connected: Boolean = false)

/**
 * Control commands over SSAP (US2/US3/US6/US7; contracts/ssap-protocol.md). Stateless except the
 * play/pause toggle, which tracks the last media action (webOS has no reliable play-state query).
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

    // --- US6: app shortcuts ---

    /**
     * Launch [app] on the TV. Resolves the real app id from listLaunchPoints titles (ids vary by
     * webOS version) and falls back to the well-known id. Returns false if the TV reports the app
     * isn't installed/launchable (US6 #3).
     */
    suspend fun launchApp(app: AppKey): Boolean {
        val id = resolveAppId(app)
        val res = manager.request("ssap://system.launcher/launch", buildJsonObject { put("id", id) })
        return res["returnValue"]?.jsonPrimitive?.booleanOrNull ?: true
    }

    private suspend fun resolveAppId(app: AppKey): String = runCatching {
        val res = manager.request("ssap://com.webos.applicationManager/listLaunchPoints")
        resolveLaunchPointId(res, app) ?: app.wellKnownId
    }.getOrDefault(app.wellKnownId)

    // --- US7: inputs ---

    suspend fun listInputs(): List<TvInput> =
        parseInputs(manager.request("ssap://tv/getExternalInputList"))

    suspend fun setInput(inputId: String) {
        manager.request("ssap://tv/switchInput", buildJsonObject { put("inputId", inputId) })
    }
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

/** Match a launch-point by title (case-insensitive) → its id. Null if not present. Pure. */
fun resolveLaunchPointId(payload: JsonObject, app: AppKey): String? {
    val points = (payload["launchPoints"] as? kotlinx.serialization.json.JsonArray) ?: return null
    return points.firstNotNullOfOrNull { el ->
        val obj = el as? JsonObject ?: return@firstNotNullOfOrNull null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
        if (title?.equals(app.title, ignoreCase = true) == true) id else null
    }
}

/** Parse getExternalInputList `{ devices: [{ id, label, connected }] }`. Pure + unit-tested. */
fun parseInputs(payload: JsonObject): List<TvInput> {
    val devices = (payload["devices"] as? kotlinx.serialization.json.JsonArray) ?: return emptyList()
    return devices.mapNotNull { el ->
        val obj = el as? JsonObject ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: id
        val connected = obj["connected"]?.jsonPrimitive?.booleanOrNull ?: false
        TvInput(id = id, label = label, connected = connected)
    }
}
