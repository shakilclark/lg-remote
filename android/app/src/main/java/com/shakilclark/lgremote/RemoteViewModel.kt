package com.shakilclark.lgremote

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.connection.NetworkMonitor
import com.shakilclark.lgremote.connection.TvConnectionManager
import com.shakilclark.lgremote.cursor.MotionCursor
import com.shakilclark.lgremote.data.TvConnection
import com.shakilclark.lgremote.data.TvStore
import com.shakilclark.lgremote.tv.Commands
import com.shakilclark.lgremote.tv.DiscoveredTv
import com.shakilclark.lgremote.tv.MediaForeground
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.NowPlaying
import com.shakilclark.lgremote.tv.PlayState
import com.shakilclark.lgremote.tv.PointerSocket
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.tv.TvDiscovery
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
    private val motionCursor = MotionCursor(
        app.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
    ) { dx, dy -> pointer.move(dx, dy) }
    private val network = NetworkMonitor(app)
    private val discovery = TvDiscovery(app)

    private val activeName = MutableStateFlow<String?>(null)
    private val hasActive = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Transient user-facing messages (e.g. a rejected command). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _inputs = MutableStateFlow<List<TvInput>>(emptyList())
    /** External inputs, loaded on demand (US7). */
    val inputs: StateFlow<List<TvInput>> = _inputs.asStateFlow()

    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    /** Installed apps from the TV (dynamic app loader); preloaded on connect. */
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    /** Live now-playing snapshot (004) — also the shared source for the future 003 lockscreen. */
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    // Now-playing is the merge of foreground-app identity (always known) + media play-state (best-effort).
    private val foregroundAppId = MutableStateFlow<String?>(null)
    private val mediaForeground = MutableStateFlow(MediaForeground(null, PlayState.Unknown))

    private val _discovered = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    /** TVs found by network scan (SSDP + port-3001 sweep). */
    val discovered: StateFlow<List<DiscoveredTv>> = _discovered.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private var volumeJob: Job? = null
    private var mediaJob: Job? = null

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
                    startNowPlaying()
                    if (_apps.value.isEmpty()) loadApps() // preload the app list
                } else {
                    stopVolumeUpdates()
                    stopNowPlaying()
                    motionCursor.stop()
                    pointer.close()
                    _inputs.value = emptyList()
                }
            }
        }
        // Off-network detection: drop to OffNetwork when the phone leaves Wi-Fi; resume on return.
        viewModelScope.launch {
            network.onLocalNetwork.collect { onLan ->
                when {
                    !onLan && hasActive.value -> manager.markOffNetwork()
                    onLan && manager.state.value is ConnectionState.OffNetwork ->
                        store.activeTv()?.let { manager.connect(it) }
                }
            }
        }
    }

    private fun startNowPlaying() {
        if (mediaJob?.isActive == true) return
        mediaJob = viewModelScope.launch {
            launch { commands.foregroundAppUpdates().collect { foregroundAppId.value = it } }
            launch { commands.mediaUpdates().collect { mediaForeground.value = it } }
            combine(foregroundAppId, mediaForeground) { appId, mf -> buildNowPlaying(appId, mf) }
                .collect { _nowPlaying.value = it }
        }
    }

    private fun stopNowPlaying() {
        mediaJob?.cancel()
        mediaJob = null
        foregroundAppId.value = null
        mediaForeground.value = MediaForeground(null, PlayState.Unknown)
        _nowPlaying.value = null
    }

    /**
     * Build the now-playing snapshot: identity from the foreground app (reliable for every app), with
     * real play-state overlaid only when the media server reports it for that same app — otherwise
     * Unknown (e.g. Netflix, which doesn't register media). Hidden on the TV home/launcher.
     */
    private fun buildNowPlaying(appId: String?, mf: MediaForeground): NowPlaying? {
        if (appId == null || appId in HOME_APP_IDS) return null
        val app = _apps.value.firstOrNull { it.id == appId }
        val playState = if (mf.appId == appId) mf.playState else PlayState.Unknown
        return NowPlaying(appId, app?.title ?: appId, app?.iconUrl, playState)
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

    /** App returned to the foreground (008) — reconnect instantly, preempting any backoff. */
    fun onForegrounded() = manager.reconnectNow()

    /** Scan for TVs on the LAN — SSDP + TCP port-3001 sweep (manual entry stays as fallback). */
    fun discover() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            try {
                _discovered.value = discovery.discover()
            } finally {
                _scanning.value = false
            }
        }
    }

    // --- US2 controls ---
    fun volumeUp() = dispatch { commands.volumeUp() }
    fun volumeDown() = dispatch { commands.volumeDown() }
    /** State-driven play/pause: uses the TV's real play-state when known, else a best-effort toggle. */
    fun playPause() = dispatch {
        when (_nowPlaying.value?.playState) {
            PlayState.Playing -> commands.pause()
            PlayState.Paused -> commands.play()
            else -> commands.playPause()
        }
    }
    fun rewind() = dispatch { commands.rewind() }
    fun fastForward() = dispatch { commands.fastForward() }
    fun stop() = dispatch { commands.stop() }

    fun toggleMute() {
        val muted = (uiState.value.connection as? ConnectionState.Connected)?.muted ?: false
        dispatch { commands.setMute(!muted) }
    }

    // --- US3 navigation (pointer-input socket) ---
    fun nav(button: NavButton) = dispatch {
        ensurePointer()
        pointer.button(button)
    }

    // --- US5 touchpad cursor (drives the US3 pointer socket directly) ---
    /** Open the pointer socket as a drag begins, so the first moves land. */
    fun cursorTouchStart() = dispatch { ensurePointer() }

    /** Send a touchpad drag delta straight to the pointer socket (no sensor, no coroutine churn). */
    fun cursorMove(dx: Int, dy: Int) {
        if (dx != 0 || dy != 0) pointer.move(dx, dy)
    }

    fun cursorClick() = dispatch {
        ensurePointer()
        pointer.click()
    }

    // --- US6 app shortcuts (dynamic loader) ---
    fun launchApp(app: TvApp) = dispatch {
        if (!commands.launchAppId(app.id)) _messages.tryEmit("${app.title} couldn't be launched")
    }

    /** Bottom-bar ⚙ — open the TV's own settings on-screen. */
    fun openTvSettings() = dispatch {
        if (!commands.openSettings()) _messages.tryEmit("Couldn't open TV settings")
    }

    /** Dynamic app loader — fetch the TV's installed apps (preloaded on connect, refreshable). */
    fun loadApps() = dispatch { _apps.value = commands.listApps() }

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
        motionCursor.stop()
        pointer.close()
        manager.disconnect()
    }

    private companion object {
        /** Foreground app ids that are the TV's own home/launcher — no now-playing strip for these. */
        val HOME_APP_IDS = setOf("com.webos.app.home")
    }
}
