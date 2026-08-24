package com.shakilclark.lgremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A remembered TV. [clientKey] is a credential — never logged, never leaves the device. */
@Serializable
data class TvConnection(
    val id: String,
    val name: String,
    val address: String,
    val clientKey: String? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lg_remote")

/**
 * App-private persistence (research R7). Stores remembered TVs + the active id. The full set is
 * small, so it's serialized as one JSON blob under a single preferences key.
 */
class TvStore(private val context: Context) {

    private val tvsKey = stringPreferencesKey("tvs_json")
    private val activeKey = stringPreferencesKey("active_id")
    private val gestureHintKey = booleanPreferencesKey("gesture_hint_seen")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val alwaysOnKey = booleanPreferencesKey("always_on_enabled")
    private val json = Json { ignoreUnknownKeys = true }

    /** Whether the one-time gesture-pad teaching card has been dismissed. */
    val gestureHintSeen: Flow<Boolean> = context.dataStore.data.map { it[gestureHintKey] ?: false }

    suspend fun setGestureHintSeen() {
        context.dataStore.edit { it[gestureHintKey] = true }
    }

    /** Reset first-run teaching (Settings → Behaviour) so the gesture-pad card shows again. */
    suspend fun resetGestureHint() {
        context.dataStore.edit { it[gestureHintKey] = false }
    }

    /** App-wide haptic feedback preference (Settings → Behaviour); defaults on. */
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[hapticsKey] ?: true }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[hapticsKey] = enabled }
    }

    /**
     * Always-on preference (Settings → Behaviour): keep the screen awake while the remote is open and
     * auto-dim it when idle, so you don't have to keep waking/unlocking the phone to nudge the TV.
     * Defaults off (it holds the screen on, which costs battery).
     */
    val alwaysOn: Flow<Boolean> = context.dataStore.data.map { it[alwaysOnKey] ?: false }

    suspend fun setAlwaysOn(enabled: Boolean) {
        context.dataStore.edit { it[alwaysOnKey] = enabled }
    }

    private val themeIdKey = stringPreferencesKey("theme_id")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    /** Selected theme + light/dark mode ids (spec 021); null → defaults (Dynamic / System). */
    val themeId: Flow<String?> = context.dataStore.data.map { it[themeIdKey] }
    val themeMode: Flow<String?> = context.dataStore.data.map { it[themeModeKey] }

    suspend fun setThemeId(id: String) {
        context.dataStore.edit { it[themeIdKey] = id }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeModeKey] = mode }
    }

    /** Forget the active TV (Settings → TV/Connection): drop it from the set and clear the active id. */
    suspend fun forgetActive() {
        context.dataStore.edit { prefs ->
            val activeId = prefs[activeKey]
            val current = prefs[tvsKey]?.let {
                runCatching { json.decodeFromString<List<TvConnection>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[tvsKey] = json.encodeToString(current.filterNot { it.id == activeId })
            prefs.remove(activeKey)
        }
    }

    val tvs: Flow<List<TvConnection>> = context.dataStore.data.map { prefs ->
        prefs[tvsKey]?.let { runCatching { json.decodeFromString<List<TvConnection>>(it) }.getOrNull() }
            ?: emptyList()
    }

    val activeId: Flow<String?> = context.dataStore.data.map { it[activeKey] }

    suspend fun activeTv(): TvConnection? {
        val id = context.dataStore.data.first()[activeKey] ?: return null
        return tvs.first().firstOrNull { it.id == id }
    }

    /** Insert or update a TV and make it active. */
    suspend fun upsertAndActivate(tv: TvConnection) {
        context.dataStore.edit { prefs ->
            val current = prefs[tvsKey]?.let {
                runCatching { json.decodeFromString<List<TvConnection>>(it) }.getOrNull()
            } ?: emptyList()
            val next = current.filterNot { it.id == tv.id } + tv
            prefs[tvsKey] = json.encodeToString(next)
            prefs[activeKey] = tv.id
        }
    }

    /** Persist a freshly-received pairing key (or an updated address) for an existing TV. */
    suspend fun updateClientKey(id: String, clientKey: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[tvsKey]?.let {
                runCatching { json.decodeFromString<List<TvConnection>>(it) }.getOrNull()
            } ?: return@edit
            prefs[tvsKey] = json.encodeToString(
                current.map { if (it.id == id) it.copy(clientKey = clientKey) else it },
            )
        }
    }

    suspend fun updateAddress(id: String, address: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[tvsKey]?.let {
                runCatching { json.decodeFromString<List<TvConnection>>(it) }.getOrNull()
            } ?: return@edit
            prefs[tvsKey] = json.encodeToString(
                current.map { if (it.id == id) it.copy(address = address) else it },
            )
        }
    }
}
