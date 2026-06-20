package com.shakilclark.lgremote.tv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * SSAP wire envelope — used for both outgoing and incoming messages.
 * Payloads are kept as a [JsonObject] because their shape varies per uri/response
 * (see specs/002-native-android-remote/contracts/ssap-protocol.md).
 */
@Serializable
data class SsapEnvelope(
    val id: String? = null,
    val type: String,
    val uri: String? = null,
    val payload: JsonObject? = null,
    val error: String? = null,
)

internal val ssapJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

object Ssap {
    /** Build a `register` message; include [clientKey] when we already have one (silent reconnect). */
    fun register(id: String, clientKey: String?): String {
        val payload = buildJsonObject {
            put("forcePairing", false)
            put("pairingType", "PROMPT")
            if (clientKey != null) put("client-key", clientKey)
            put("manifest", REGISTER_MANIFEST)
        }
        return ssapJson.encodeToString(
            SsapEnvelope.serializer(),
            SsapEnvelope(id = id, type = "register", payload = payload),
        )
    }

    /** Build a `request` (or `subscribe`) message for an ssap:// uri. */
    fun request(id: String, uri: String, payload: JsonObject? = null, subscribe: Boolean = false): String =
        ssapJson.encodeToString(
            SsapEnvelope.serializer(),
            SsapEnvelope(
                id = id,
                type = if (subscribe) "subscribe" else "request",
                uri = uri,
                payload = payload,
            ),
        )

    fun parse(text: String): SsapEnvelope = ssapJson.decodeFromString(SsapEnvelope.serializer(), text)
}

/** Permission manifest the TV uses to scope what this client may do (standard webOS set). */
private val REGISTER_MANIFEST: JsonObject = buildJsonObject {
    put("manifestVersion", 1)
    put(
        "permissions",
        ssapJson.parseToJsonElement(
            """[
              "LAUNCH","LAUNCH_WEBAPP","APP_TO_APP","CONTROL_AUDIO",
              "CONTROL_INPUT_MEDIA_PLAYBACK","CONTROL_POWER","READ_INSTALLED_APPS",
              "CONTROL_DISPLAY","CONTROL_INPUT_JOYSTICK","CONTROL_INPUT_MEDIA_RECORDING",
              "CONTROL_INPUT_TV","READ_INPUT_DEVICE_LIST","READ_NETWORK_STATE",
              "READ_TV_CHANNEL_LIST","WRITE_NOTIFICATION_TOAST","CONTROL_INPUT_TEXT",
              "CONTROL_MOUSE_AND_KEYBOARD","READ_CURRENT_CHANNEL","READ_RUNNING_APPS"
            ]""",
        ),
    )
}

/** D-pad / pointer-socket buttons (US3). */
enum class NavButton { UP, DOWN, LEFT, RIGHT, ENTER, BACK, HOME, EXIT }
