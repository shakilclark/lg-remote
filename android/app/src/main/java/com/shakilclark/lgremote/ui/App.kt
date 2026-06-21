package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.UiState
import androidx.compose.material3.MaterialTheme
import com.shakilclark.lgremote.connection.ConnectionState

/**
 * Root routing (T019). Mirrors the 001 flow: a known TV that's reconnecting shows the reconnect
 * view (not the scanner); an unconfigured app shows ConnectScreen; connected shows the remote.
 */
@Composable
fun App(
    ui: UiState,
    onConnect: (address: String, name: String) -> Unit,
    onRetry: () -> Unit,
    onVolumeUp: () -> Unit = {},
    onVolumeDown: () -> Unit = {},
    onChannelUp: () -> Unit = {},
    onChannelDown: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onRewind: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onFastForward: () -> Unit = {},
    onNav: (com.shakilclark.lgremote.tv.NavButton) -> Unit = {},
    apps: List<com.shakilclark.lgremote.tv.TvApp> = emptyList(),
    appsLoading: Boolean = false,
    onLaunchApp: (com.shakilclark.lgremote.tv.TvApp) -> Unit = {},
    inputs: List<com.shakilclark.lgremote.tv.TvInput> = emptyList(),
    onLoadInputs: () -> Unit = {},
    onSelectInput: (com.shakilclark.lgremote.tv.TvInput) -> Unit = {},
    onCursorTouchStart: () -> Unit = {},
    onCursorMove: (dx: Int, dy: Int) -> Unit = { _, _ -> },
    onCursorClick: () -> Unit = {},
    onOpenTvSettings: () -> Unit = {},
    nowPlaying: com.shakilclark.lgremote.tv.NowPlaying? = null,
    onStop: () -> Unit = {},
    discovered: List<com.shakilclark.lgremote.tv.DiscoveredTv> = emptyList(),
    scanning: Boolean = false,
    onScan: () -> Unit = {},
    onPowerOff: () -> Unit = {},
    showGestureHint: Boolean = false,
    onDismissGestureHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var reconfigure by remember { mutableStateOf(false) }
    val conn = ui.connection
    val connected = conn is ConnectionState.Connected

    // 008 seamless reconnect: after a brief drop, keep showing the last-known remote (with a corner
    // "reconnecting" chip) for a grace window instead of flashing the reconnect screen.
    var lastConnected by remember { mutableStateOf<ConnectionState.Connected?>(null) }
    var graceExpired by remember { mutableStateOf(false) }
    LaunchedEffect(connected) {
        if (connected) { lastConnected = conn as ConnectionState.Connected; graceExpired = false }
    }
    val briefDrop = conn is ConnectionState.Connecting || conn is ConnectionState.Disconnected
    val inGrace = !connected && !reconfigure && lastConnected != null && briefDrop && !graceExpired
    LaunchedEffect(inGrace) { if (inGrace) { delay(2_500); graceExpired = true } }

    val transient = conn is ConnectionState.Connecting ||
        conn is ConnectionState.Disconnected ||
        conn is ConnectionState.OffNetwork
    val showReconnect = !connected && ui.hasActiveTv && transient && !reconfigure && !inGrace

    // The remote to render: live state when connected, last-known during the grace window.
    val remoteState = (conn as? ConnectionState.Connected) ?: lastConnected

    Column(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Banner only when there's something to say and we're not keeping the remote up mid-reconnect.
        if (!connected && !inGrace) {
            ConnectionBanner(state = conn, tvName = ui.activeTvName, hasActiveTv = ui.hasActiveTv)
        }
        when {
            reconfigure -> ConnectScreen(
                state = conn,
                onConnect = { a, n -> reconfigure = false; onConnect(a, n) },
                discovered = discovered,
                scanning = scanning,
                onScan = onScan,
            )
            (connected || inGrace) && remoteState != null -> RemoteScreen(
                state = remoteState,
                reconnecting = inGrace,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                onChannelUp = onChannelUp,
                onChannelDown = onChannelDown,
                onToggleMute = onToggleMute,
                onRewind = onRewind,
                onPlayPause = onPlayPause,
                onFastForward = onFastForward,
                onNav = onNav,
                apps = apps,
                appsLoading = appsLoading,
                onLaunchApp = onLaunchApp,
                inputs = inputs,
                onLoadInputs = onLoadInputs,
                onSelectInput = onSelectInput,
                onCursorTouchStart = onCursorTouchStart,
                onCursorMove = onCursorMove,
                onCursorClick = onCursorClick,
                onOpenTvSettings = onOpenTvSettings,
                nowPlaying = nowPlaying,
                onStop = onStop,
                tvName = ui.activeTvName ?: "",
                onPowerOff = onPowerOff,
                showGestureHint = showGestureHint,
                onDismissGestureHint = onDismissGestureHint,
            )
            showReconnect -> ReconnectView(onRetry = onRetry, onChange = { reconfigure = true })
            else -> ConnectScreen(
                state = conn,
                onConnect = { a, n -> reconfigure = false; onConnect(a, n) },
                discovered = discovered,
                scanning = scanning,
                onScan = onScan,
            )
        }
    }
}

@Composable
private fun ReconnectView(onRetry: () -> Unit, onChange: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("📺", fontSize = 46.sp)
        Text("Can't reach your TV", fontSize = 22.sp)
        Text(
            "It may be off or asleep. It'll reconnect automatically when it's back.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Try now") }
        Button(onClick = onChange) { Text("Choose a different TV") }
    }
}
