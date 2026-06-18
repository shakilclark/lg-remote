package com.shakilclark.lgremote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.connection.TvConnectionManager
import com.shakilclark.lgremote.data.TvConnection
import com.shakilclark.lgremote.data.TvStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether the UI should show the remote, the reconnect view, or the connect/pair screen. */
data class UiState(
    val connection: ConnectionState = ConnectionState.Disconnected(),
    val activeTvName: String? = null,
    val hasActiveTv: Boolean = false,
)

/**
 * Skeleton (T013) — owns the [TvConnectionManager] and [TvStore], exposes a single [UiState],
 * and auto-connects to a remembered TV on launch. US1 (pairing) builds on this.
 */
class RemoteViewModel(app: Application) : AndroidViewModel(app) {

    private val store = TvStore(app)
    private val manager = TvConnectionManager(
        scope = viewModelScope,
        persistClientKey = { id, key -> store.updateClientKey(id, key) },
    )

    private val activeName = MutableStateFlow<String?>(null)
    private val hasActive = MutableStateFlow(false)

    val uiState: StateFlow<UiState> =
        combine(manager.state, activeName, hasActive) { conn, name, has ->
            UiState(connection = conn, activeTvName = name, hasActiveTv = has)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        viewModelScope.launch {
            store.activeTv()?.let { tv ->
                activeName.value = tv.name
                hasActive.value = true
                manager.connect(tv)
            }
        }
    }

    /** Add a TV by address and begin pairing (fleshed out in US1). */
    fun connectTo(address: String, name: String) {
        viewModelScope.launch {
            val tv = TvConnection(id = address, name = name, address = address)
            store.upsertAndActivate(tv)
            activeName.value = name
            hasActive.value = true
            manager.connect(tv)
        }
    }

    fun retry() {
        viewModelScope.launch { store.activeTv()?.let { manager.connect(it) } }
    }
}
