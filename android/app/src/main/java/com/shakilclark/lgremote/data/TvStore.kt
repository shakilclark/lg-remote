package com.shakilclark.lgremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    private val json = Json { ignoreUnknownKeys = true }

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
