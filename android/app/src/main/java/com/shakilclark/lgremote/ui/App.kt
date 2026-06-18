package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.UiState
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.Muted

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
    onToggleMute: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNav: (com.shakilclark.lgremote.tv.NavButton) -> Unit = {},
    onLaunchApp: (com.shakilclark.lgremote.tv.AppKey) -> Unit = {},
    inputs: List<com.shakilclark.lgremote.tv.TvInput> = emptyList(),
    onLoadInputs: () -> Unit = {},
    onSelectInput: (com.shakilclark.lgremote.tv.TvInput) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var reconfigure by remember { mutableStateOf(false) }
    val conn = ui.connection
    val connected = conn is ConnectionState.Connected
    val transient = conn is ConnectionState.Connecting ||
        conn is ConnectionState.Disconnected ||
        conn is ConnectionState.OffNetwork
    val showReconnect = !connected && ui.hasActiveTv && transient && !reconfigure

    Column(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ConnectionBanner(state = conn, tvName = ui.activeTvName)
        when {
            connected -> RemoteScreen(
                state = conn as ConnectionState.Connected,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                onToggleMute = onToggleMute,
                onPlayPause = onPlayPause,
                onNav = onNav,
                onLaunchApp = onLaunchApp,
                inputs = inputs,
                onLoadInputs = onLoadInputs,
                onSelectInput = onSelectInput,
            )
            showReconnect -> ReconnectView(onRetry = onRetry, onChange = { reconfigure = true })
            else -> ConnectScreen(state = conn, onConnect = { a, n -> reconfigure = false; onConnect(a, n) })
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
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Try now") }
        Button(onClick = onChange) { Text("Choose a different TV") }
    }
}
