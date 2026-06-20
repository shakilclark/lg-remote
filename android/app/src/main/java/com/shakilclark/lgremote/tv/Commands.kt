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

/** An installed app reported by the TV's launch points (dynamic app loader). */
data class TvApp(val id: String, val title: String, val iconUrl: String? = null)

/**
 * Control commands over SSAP (US2/US3/US6/US7; contracts/ssap-protocol.md). Stateless except the
 * play/pause toggle, which tracks the last media action (webOS has no reliable play-state query).
 */
class Commands(private val manager: TvConnectionManager) {

    private var playing: Boolean? = null

    suspend fun volumeUp() { manager.request("ssap://audio/volumeUp") }
    suspend fun volumeDown() { manager.request("ssap://audio/volumeDown") }

    suspend fun channelUp() { manager.request("ssap://tv/channelUp") }
    suspend fun channelDown() { manager.request("ssap://tv/channelDown") }

    suspend fun setMute(mute: Boolean) {
        manager.request("ssap://audio/setMute", buildJsonObject { put("mute", mute) })
    }

    suspend fun play() { manager.request("ssap://media.controls/play"); playing = true }
    suspend fun pause() { manager.request("ssap://media.controls/pause"); playing = false }

    /** Toggle; default to pause when the state is unknown (matches the 001 behaviour). */
    suspend fun playPause() { if (playing == true) pause() else play() }

    suspend fun rewind() { manager.request("ssap://media.controls/rewind") }
    suspend fun fastForward() { manager.request("ssap://media.controls/fastForward") }
    suspend fun stop() { manager.request("ssap://media.controls/stop"); playing = false }

    /**
     * Live foreground-app id (004) — the reliable "what's on" signal, present for every app/source
     * (e.g. `netflix`, `youtube.leanback.v4`, `com.webos.app.hdmi1`). Subscribable.
     */
    fun foregroundAppUpdates(): Flow<String?> =
        (manager.subscribe("ssap://com.webos.applicationManager/getForegroundAppInfo") ?: emptyFlow())
            .map { it["appId"]?.jsonPrimitive?.contentOrNull?.takeIf { id -> id.isNotBlank() } }

    /**
     * Live media play-state (004), where the app registers with the webOS media server — many apps
     * (Netflix, etc.) report an empty array, so this is best-effort; identity comes from
     * [foregroundAppUpdates].
     */
    fun mediaUpdates(): Flow<MediaForeground> =
        (manager.subscribe("ssap://com.webos.media/getForegroundAppInfo") ?: emptyFlow())
            .map { parseMediaForeground(it) }

    /** Live volume/mute stream; empty when not connected. */
    fun volumeUpdates(): Flow<VolumeState> =
        (manager.subscribe("ssap://audio/getVolume") ?: emptyFlow()).map { parseVolume(it) }

    // --- US6: app shortcuts (dynamic loader) ---

    /** All installed apps from the TV's launch points, in the TV's own order (dynamic app loader). */
    suspend fun listApps(): List<TvApp> =
        parseLaunchPoints(manager.request("ssap://com.webos.applicationManager/listLaunchPoints"))

    /** Launch an app by its resolved launch-point id. */
    suspend fun launchAppId(id: String): Boolean {
        val res = manager.request("ssap://system.launcher/launch", buildJsonObject { put("id", id) })
        return res["returnValue"]?.jsonPrimitive?.booleanOrNull ?: true
    }

    /**
     * Open the TV's own settings on-screen (bottom-bar ⚙). Launches the webOS settings app via the
     * same launcher used for app shortcuts — resolved by title where possible, falling back to the
     * well-known settings app id. Returns false if the TV won't launch it.
     */
    suspend fun openSettings(): Boolean {
        val id = runCatching {
            val res = manager.request("ssap://com.webos.applicationManager/listLaunchPoints")
            resolveLaunchPointIdByTitle(res, "Settings")
        }.getOrNull() ?: SETTINGS_APP_ID
        val res = manager.request("ssap://system.launcher/launch", buildJsonObject { put("id", id) })
        return res["returnValue"]?.jsonPrimitive?.booleanOrNull ?: true
    }

    private companion object {
        /** webOS settings app id; used if the TV's launch-point list has no "Settings" title. */
        const val SETTINGS_APP_ID = "com.palm.app.settings"
    }

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

/**
 * Parse `listLaunchPoints` → the installed apps in the TV's order. Keeps id + title + the raw icon
 * string (may be a fetchable URL or a non-servable path — the UI loads it and falls back to a tile).
 * Pure + unit-tested.
 */
fun parseLaunchPoints(payload: JsonObject): List<TvApp> {
    val points = (payload["launchPoints"] as? kotlinx.serialization.json.JsonArray) ?: return emptyList()
    return points.mapNotNull { el ->
        val obj = el as? JsonObject ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: id
        val icon = obj["icon"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        TvApp(id = id, title = title, iconUrl = icon)
    }
}

/** Match a launch-point by title (case-insensitive) → its id. Null if not present. Pure. */
fun resolveLaunchPointIdByTitle(payload: JsonObject, title: String): String? {
    val points = (payload["launchPoints"] as? kotlinx.serialization.json.JsonArray) ?: return null
    return points.firstNotNullOfOrNull { el ->
        val obj = el as? JsonObject ?: return@firstNotNullOfOrNull null
        val pointTitle = obj["title"]?.jsonPrimitive?.contentOrNull
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
        if (pointTitle?.equals(title, ignoreCase = true) == true) id else null
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
