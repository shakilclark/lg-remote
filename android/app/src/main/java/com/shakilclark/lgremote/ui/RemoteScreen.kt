package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
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
    reconnecting: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showPad by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var showInputs by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        // Now-playing strip — only when something is actually playing (tap to expand).
        if (nowPlaying != null) {
            NowPlayingBar(
                nowPlaying = nowPlaying,
                onExpand = { showNowPlaying = true },
                onRewind = onRewind,
                onPlayPause = onPlayPause,
                onFastForward = onFastForward,
                onStop = onStop,
                modifier = Modifier.padding(horizontal = Space.l, vertical = Space.s),
            )
        }
        // Body — D-pad biased low (thumb arc); transport + volume as two centered rows beneath it.
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f)) // bigger top gap → cluster sits in the lower half
            DirectionPad(onNav = onNav, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            VolumeRow(
                onUp = onVolumeUp,
                onDown = onVolumeDown,
                muted = state.muted,
                onToggleMute = onToggleMute,
                modifier = Modifier.padding(bottom = Space.s),
            )
        }

        BottomBar(
            onBack = { onNav(NavButton.BACK) },
            onHome = { onNav(NavButton.HOME) },
            onOpenPad = { showPad = true },
            onOpenApps = { showApps = true },
            onOpenInputs = { onLoadInputs(); showInputs = true },
            onOpenTvSettings = onOpenTvSettings,
        )
      }
      if (reconnecting) {
          ReconnectingChip(Modifier.align(Alignment.TopEnd).padding(Space.m))
      }
    }

    if (showPad) {
        ModalBottomSheet(
            onDismissRequest = { showPad = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            GesturePad(
                onTouchStart = onCursorTouchStart,
                onMove = onCursorMove,
                onClick = onCursorClick,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                onChannelUp = onChannelUp,
                onChannelDown = onChannelDown,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .padding(horizontal = Space.l)
                    .padding(bottom = Space.xxl),
            )
        }
    }

    if (showApps) {
        ModalBottomSheet(
            onDismissRequest = { showApps = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Text(
                    "Apps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Space.s),
                )
                AppGrid(
                    apps = apps,
                    onLaunch = { onLaunchApp(it); showApps = false },
                    modifier = Modifier.fillMaxWidth().height(440.dp),
                )
            }
        }
    }

    if (showInputs) {
        ModalBottomSheet(
            onDismissRequest = { showInputs = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            InputSwitcher(
                inputs = inputs.filter { it.connected },
                onSelect = { onSelectInput(it); showInputs = false },
                modifier = Modifier.padding(horizontal = Space.xl).padding(bottom = Space.xxl),
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
        RemoteScreen(ConnectionState.Connected(volume = 13, muted = false), {}, {}, {})
    }
}
