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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.NowPlaying
import com.shakilclark.lgremote.ui.components.MaterialSymbols
import com.shakilclark.lgremote.ui.components.SymbolIcon
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space
import kotlinx.coroutines.launch

/** Peek height of the command sheet at rest — sized to the grip handle so no sheet content shows. */
private val GRIP_PEEK = 48.dp

/**
 * The connected remote (Direction C). Top to bottom: the connection top bar (chip + voice + power),
 * an optional now-playing bar when media is live (tap to expand the cover sheet), the gesture pad as
 * the primary surface (glide to point, tap to click; right edge = volume; bottom-left = back), and a
 * grip that pulls up the command sheet (Keypad / Apps / Inputs / Sound / Type). Inputs are filtered to
 * connected sources. One screen, no scroll.
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
    appsLoading: Boolean = false,
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
    onOpenSettings: () -> Unit = {},
    reconnecting: Boolean = false,
    showGestureHint: Boolean = false,
    onDismissGestureHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showNowPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Standard (non-modal) bottom sheet: the grip is its drag handle at a peek height, so dragging it
    // tracks the finger 1:1 and flings/settles open or closed (spec 023 — replaces the laggy
    // open-on-threshold ModalBottomSheet). Never fully hidden → the grip is always a visible promise.
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    // Pull the TV inputs the moment the sheet begins to open.
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue }.collect { if (it == SheetValue.Expanded) onLoadInputs() }
    }
    fun toggleSheet() {
        scope.launch {
            if (sheetState.currentValue == SheetValue.Expanded) sheetState.partialExpand()
            else sheetState.expand()
        }
    }

    Box(modifier.fillMaxSize()) {
      BottomSheetScaffold(
          scaffoldState = scaffoldState,
          sheetPeekHeight = GRIP_PEEK,
          sheetDragHandle = { Grip(onTap = ::toggleSheet) },
          sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          containerColor = MaterialTheme.colorScheme.surface,
          sheetContent = {
              CommandSheet(
                  apps = apps,
                  appsLoading = appsLoading,
                  onLaunchApp = { onLaunchApp(it); scope.launch { sheetState.partialExpand() } },
                  inputs = inputs,
                  onSelectInput = { onSelectInput(it); scope.launch { sheetState.partialExpand() } },
                  modifier = Modifier.fillMaxWidth().padding(bottom = Space.xxl),
              )
          },
      ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(bottom = GRIP_PEEK)) {
            // Top bar: connection chip + More-controls (Tune) + voice (inactive shell) + power.
            RemoteTopBar(
                tvName = tvName,
                onPowerOff = onPowerOff,
                onOpenSettings = onOpenSettings,
                onOpenMore = ::toggleSheet,
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
            // Home (Direction C): the gesture pad is the primary surface — glide/tap to point & click.
            GesturePad(
                onTouchStart = onCursorTouchStart,
                onMove = onCursorMove,
                onClick = onCursorClick,
                onNav = onNav,
                onMute = onToggleMute,
                onOpenTvSettings = onOpenTvSettings,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                muted = state.muted == true,
                showHint = showGestureHint,
                onDismissHint = onDismissGestureHint,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.l, vertical = Space.s),
            )
        }
      }
      if (reconnecting) {
          ReconnectingChip(Modifier.align(Alignment.TopEnd).padding(Space.m))
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

/**
 * The command sheet's **drag handle** (spec 023): a visible grip + "More controls" label. The host
 * [BottomSheetScaffold] makes the sheet track the finger when you drag this handle up/down and settle
 * open/closed by velocity; a **tap** toggles it via [onTap]. Because it lives on the sheet (below the
 * clickpad), it can't steal the pad's nav-taps / cursor glides.
 */
@Composable
private fun Grip(onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onTap)
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
        SymbolIcon(
            MaterialSymbols.Sync,
            contentDescription = null,
            size = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
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
