package com.shakilclark.lgremote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.connection.TvConnectionManager
import com.shakilclark.lgremote.data.TvConnection
import com.shakilclark.lgremote.data.TvStore
import com.shakilclark.lgremote.tv.AppKey
import com.shakilclark.lgremote.tv.Commands
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.PointerSocket
import com.shakilclark.lgremote.tv.TvInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

/** Whether the UI should show the remote, the reconnect view, or the connect/pair screen. */
data class UiState(
    val connection: ConnectionState = ConnectionState.Disconnected(),
    val activeTvName: String? = null,
    val hasActiveTv: Boolean = false,
)

/**
 * Owns the [TvConnectionManager], [TvStore], [Commands], and the [PointerSocket]. Exposes a
 * single [UiState], auto-connects on launch (US1), dispatches control/navigation/app/input
 * commands, and keeps live volume/mute folded into the Connected state.
 */
class RemoteViewModel(app: Application) : AndroidViewModel(app) {

    private val store = TvStore(app)
    private val manager = TvConnectionManager(
        scope = viewModelScope,
        persistClientKey = { id, key -> store.updateClientKey(id, key) },
    )
    private val commands = Commands(manager)
    private val pointer = PointerSocket()

    private val activeName = MutableStateFlow<String?>(null)
    private val hasActive = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Transient user-facing messages (e.g. a rejected command). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _inputs = MutableStateFlow<List<TvInput>>(emptyList())
    /** External inputs, loaded on demand (US7). */
    val inputs: StateFlow<List<TvInput>> = _inputs.asStateFlow()

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
        viewModelScope.launch {
            manager.state.collect { state ->
                if (state is ConnectionState.Connected) {
                    startVolumeUpdates()
                } else {
                    stopVolumeUpdates()
                    pointer.close()
                    _inputs.value = emptyList()
                }
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

    private suspend fun ensurePointer() {
        if (pointer.isOpen) return
        val payload = manager.request("ssap://com.webos.service.networkinput/getPointerInputSocket")
        val path = payload["socketPath"]?.jsonPrimitive?.content ?: return
        pointer.connect(path)
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

    // --- US3 navigation (pointer-input socket) ---
    fun nav(button: NavButton) = dispatch {
        ensurePointer()
        pointer.button(button)
    }

    // --- US6 app shortcuts ---
    fun launchApp(app: AppKey) = dispatch {
        if (!commands.launchApp(app)) _messages.tryEmit("${app.title} isn't installed on this TV")
    }

    // --- US7 inputs ---
    fun loadInputs() = dispatch { _inputs.value = commands.listInputs() }
    fun setInput(inputId: String) = dispatch {
        commands.setInput(inputId)
        _inputs.value = commands.listInputs()
    }

    private fun dispatch(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onFailure {
                _messages.tryEmit(it.message ?: "TV not connected")
            }
        }
    }

    override fun onCleared() {
        pointer.close()
        manager.disconnect()
    }
}
