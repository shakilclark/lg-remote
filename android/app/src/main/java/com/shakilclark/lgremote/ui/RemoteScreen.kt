package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.AppKey
import com.shakilclark.lgremote.tv.NavButton
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
    onToggleMute: () -> Unit,
    onRewind: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onFastForward: () -> Unit = {},
    onNav: (NavButton) -> Unit = {},
    onLaunchApp: (AppKey) -> Unit = {},
    inputs: List<TvInput> = emptyList(),
    onLoadInputs: () -> Unit = {},
    onSelectInput: (TvInput) -> Unit = {},
    onCursorTouchStart: () -> Unit = {},
    onCursorMove: (dx: Int, dy: Int) -> Unit = { _, _ -> },
    onCursorClick: () -> Unit = {},
    onOpenTvSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showPad by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var showInputs by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        // Body — D-pad biased low (thumb arc); transport + volume as two centered rows beneath it.
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f)) // bigger top gap → cluster sits in the lower half
            DirectionPad(onNav = onNav, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            TransportRow(
                onRewind = onRewind,
                onPlayPause = onPlayPause,
                onFastForward = onFastForward,
            )
            Spacer(Modifier.height(Space.m))
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

    if (showPad) {
        ModalBottomSheet(
            onDismissRequest = { showPad = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TouchPad(
                onTouchStart = onCursorTouchStart,
                onMove = onCursorMove,
                onClick = onCursorClick,
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
                Modifier.fillMaxWidth().padding(horizontal = Space.xl).padding(bottom = Space.xxl),
                verticalArrangement = Arrangement.spacedBy(Space.l),
            ) {
                Text("Apps", style = MaterialTheme.typography.titleMedium)
                AppShortcuts(onLaunch = { onLaunchApp(it); showApps = false })
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
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun RemotePreview() {
    LGRemoteTheme {
        RemoteScreen(ConnectionState.Connected(volume = 13, muted = false), {}, {}, {})
    }
}
