package com.shakilclark.lgremote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.connection.TvConnectionManager
import com.shakilclark.lgremote.data.TvConnection
import com.shakilclark.lgremote.data.TvStore
import com.shakilclark.lgremote.tv.Commands
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * Owns the [TvConnectionManager], [TvStore], and [Commands]. Exposes a single [UiState],
 * auto-connects a remembered TV on launch (US1), dispatches control commands, and keeps live
 * volume/mute folded into the Connected state (US2).
 */
class RemoteViewModel(app: Application) : AndroidViewModel(app) {

    private val store = TvStore(app)
    private val manager = TvConnectionManager(
        scope = viewModelScope,
        persistClientKey = { id, key -> store.updateClientKey(id, key) },
    )
    private val commands = Commands(manager)

    private val activeName = MutableStateFlow<String?>(null)
    private val hasActive = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Transient user-facing messages (e.g. a rejected command). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var volumeJob: Job? = null

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
        // Restart the volume subscription each time we (re)connect; stop otherwise.
        viewModelScope.launch {
            manager.state.collect { state ->
                if (state is ConnectionState.Connected) startVolumeUpdates() else stopVolumeUpdates()
            }
        }
    }

    private fun startVolumeUpdates() {
        if (volumeJob?.isActive == true) return
        volumeJob = viewModelScope.launch {
            commands.volumeUpdates().collect { v -> manager.applyVolume(v.volume, v.muted) }
        }
    }

    private fun stopVolumeUpdates() {
        volumeJob?.cancel()
        volumeJob = null
    }

    fun connectTo(address: String, name: String) {
        viewModelScope.launch {
            val tv = TvConnection(id = address, name = name, address = address)
            store.upsertAndActivate(tv)
            activeName.value = name
            hasActive.value = true
            manager.connect(tv)
        }
    }

    fun retry() = viewModelScope.launch { store.activeTv()?.let { manager.connect(it) } }

    // --- US2 controls ---
    fun volumeUp() = dispatch { commands.volumeUp() }
    fun volumeDown() = dispatch { commands.volumeDown() }
    fun playPause() = dispatch { commands.playPause() }

    fun toggleMute() {
        val muted = (uiState.value.connection as? ConnectionState.Connected)?.muted ?: false
        dispatch { commands.setMute(!muted) }
    }

    private fun dispatch(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onFailure {
                _messages.tryEmit(it.message ?: "TV not connected")
            }
        }
    }
}
