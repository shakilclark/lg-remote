package com.shakilclark.lgremote.tv

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Real playback state reported by the TV's media server (US5/004). */
enum class PlayState { Playing, Paused, Stopped, Unknown }

/** Raw media-server foreground info: which app has media, and its play-state. */
data class MediaForeground(val appId: String?, val playState: PlayState)

/**
 * What the TV is showing right now (004 now-playing). Resolved from [MediaForeground] + the app
 * list: the app's display name + icon, and its play-state. This is the single snapshot the
 * now-playing surface renders and the `003` lockscreen will consume. Null when nothing is playing.
 */
data class NowPlaying(
    val appId: String,
    val name: String,
    val iconUrl: String?,
    val playState: PlayState,
)

/**
 * Parse `com.webos.media/getForegroundAppInfo` → the first foreground media entry's app id +
 * play-state. Empty array (no app reporting media) → null appId / Unknown. Pure + unit-tested.
 */
fun parseMediaForeground(payload: JsonObject): MediaForeground {
    val first = (payload["foregroundAppInfo"] as? JsonArray)?.firstOrNull() as? JsonObject
    val appId = first?.get("appId")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    val state = when (first?.get("playState")?.jsonPrimitive?.contentOrNull?.lowercase()) {
        "playing" -> PlayState.Playing
        "paused" -> PlayState.Paused
        "stopped", "unloaded" -> PlayState.Stopped
        else -> PlayState.Unknown
    }
    return MediaForeground(appId, if (appId == null) PlayState.Unknown else state)
}
