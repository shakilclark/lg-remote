package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.NowPlaying
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space

/**
 * The connected remote (redesign). Body holds a full-width +-shaped D-pad biased toward the lower
 * half (right-thumb arc) and a vertical volume strip in the bottom-right corner; everything else is
 * the persistent bottom bar. Pad / Apps / Inputs open as bottom sheets; Settings launches the TV's
 * own settings directly. Inputs are filtered to connected sources. One screen, no scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    state: ConnectionState.Connected,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onChannelUp: () -> Unit = {},
    onChannelDown: () -> Unit = {},
    onToggleMute: () -> Unit,
    onRewind: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onFastForward: () -> Unit = {},
    onNav: (NavButton) -> Unit = {},
    apps: List<TvApp> = emptyList(),
    onLaunchApp: (TvApp) -> Unit = {},
    inputs: List<TvInput> = emptyList(),
    onLoadInputs: () -> Unit = {},
    onSelectInput: (TvInput) -> Unit = {},
    onCursorTouchStart: () -> Unit = {},
    onCursorMove: (dx: Int, dy: Int) -> Unit = { _, _ -> },
    onCursorClick: () -> Unit = {},
    onOpenTvSettings: () -> Unit = {},
    nowPlaying: NowPlaying? = null,
    onStop: () -> Unit = {},
    tvName: String = "",
    onPowerOff: () -> Unit = {},
    reconnecting: Boolean = false,
    showGestureHint: Boolean = false,
    onDismissGestureHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showCommands by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        // Top bar: connection chip + voice (inactive shell) + power.
        RemoteTopBar(
            tvName = tvName,
            onPowerOff = onPowerOff,
            modifier = Modifier.padding(horizontal = Space.l, vertical = Space.s),
        )
        // Now-playing strip — only when something is actually playing (tap to expand).
        if (nowPlaying != null) {
            NowPlayingBar(
                nowPlaying = nowPlaying,
                onExpand = { showNowPlaying = true },
                onPlayPause = onPlayPause,
                modifier = Modifier.padding(horizontal = Space.l, vertical = Space.s),
            )
        }
        // Home (Direction C): the gesture pad is the primary surface — glide/tap to point & click,
        // right edge = volume, left edge = channel.
        GesturePad(
            onTouchStart = onCursorTouchStart,
            onMove = onCursorMove,
            onClick = onCursorClick,
            onVolumeUp = onVolumeUp,
            onVolumeDown = onVolumeDown,
            onChannelUp = onChannelUp,
            onChannelDown = onChannelDown,
            showHint = showGestureHint,
            onDismissHint = onDismissGestureHint,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.l, vertical = Space.s),
        )
        // Grip — pulls up the command sheet (keys / apps / inputs / mute / settings). Back is
        // intentionally not surfaced on the home yet — that affordance is being redesigned.
        Grip(
            onOpen = { onLoadInputs(); showCommands = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = Space.s),
        )
      }
      if (reconnecting) {
          ReconnectingChip(Modifier.align(Alignment.TopEnd).padding(Space.m))
      }
    }

    if (showCommands) {
        ModalBottomSheet(
            onDismissRequest = { showCommands = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            CommandSheet(
                onNav = onNav,
                muted = state.muted == true,
                onToggleMute = onToggleMute,
                apps = apps,
                onLaunchApp = { onLaunchApp(it); showCommands = false },
                inputs = inputs,
                onSelectInput = { onSelectInput(it); showCommands = false },
                onOpenTvSettings = onOpenTvSettings,
                modifier = Modifier.padding(bottom = Space.xxl),
            )
        }
    }

    if (showNowPlaying && nowPlaying != null) {
        ModalBottomSheet(
            onDismissRequest = { showNowPlaying = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            NowPlayingSheetContent(
                nowPlaying = nowPlaying,
                onRewind = onRewind,
                onPlayPause = onPlayPause,
                onFastForward = onFastForward,
                onStop = onStop,
                modifier = Modifier.padding(horizontal = Space.xl).padding(bottom = Space.xxl),
            )
        }
    }
}

/** The bottom grip that raises the command sheet — the only on-screen promise that there's more. */
@Composable
private fun Grip(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = "More controls" }
            .padding(vertical = Space.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Box(
            Modifier.size(width = 40.dp, height = 5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline),
        )
        Text(
            "More controls",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Subtle top-right chip shown during the 008 reconnect grace window. */
@Composable
private fun ReconnectingChip(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(Space.s)
            .semantics { contentDescription = "Reconnecting" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Sync,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun RemotePreview() {
    LGRemoteTheme {
        RemoteScreen(
            state = ConnectionState.Connected(volume = 13, muted = false),
            onVolumeUp = {},
            onVolumeDown = {},
            onToggleMute = {},
        )
    }
}
