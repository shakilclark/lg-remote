package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.NowPlaying
import com.shakilclark.lgremote.tv.PlayState
import com.shakilclark.lgremote.ui.components.ControlKey
import com.shakilclark.lgremote.ui.theme.Space

private fun stateLabel(state: PlayState): String = when (state) {
    PlayState.Playing -> "Playing"
    PlayState.Paused -> "Paused"
    PlayState.Stopped -> "Stopped"
    PlayState.Unknown -> "Playback state unknown"
}

/**
 * Slim now-playing strip for the top of the remote (004) — shown only when an app is actually
 * playing/paused. Shows the app icon + name + play-state; tap to expand the full sheet.
 */
@Composable
fun NowPlayingStrip(nowPlaying: NowPlaying, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val loader = rememberTvImageLoader()
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onExpand()
            }
            .semantics { contentDescription = "Now playing: ${nowPlaying.name}" }
            .padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(
            Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(nowPlaying.iconUrl, nowPlaying.name, loader, Modifier.fillMaxSize().padding(Space.xs))
        }
        Column(Modifier.weight(1f)) {
            Text(
                nowPlaying.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stateLabel(nowPlaying.playState),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.ExpandLess, contentDescription = "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Now-playing sheet body (004): larger app identity + play-state + the full SSAP transport set
 * including Stop. The play/pause glyph reflects real state. Wrapped in a ModalBottomSheet by the
 * caller, matching the Apps/Inputs pattern.
 */
@Composable
fun NowPlayingSheetContent(
    nowPlaying: NowPlaying,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onFastForward: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loader = rememberTvImageLoader()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.l)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(64.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(nowPlaying.iconUrl, nowPlaying.name, loader, Modifier.fillMaxSize().padding(Space.s))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    nowPlaying.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stateLabel(nowPlaying.playState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.l, Alignment.CenterHorizontally),
        ) {
            TransKey(Icons.Filled.FastRewind, "Rewind", onRewind)
            TransKey(
                if (nowPlaying.playState == PlayState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                "Play or pause",
                onPlayPause,
            )
            TransKey(Icons.Filled.FastForward, "Fast forward", onFastForward)
            TransKey(Icons.Filled.Stop, "Stop", onStop)
        }
    }
}

@Composable
private fun TransKey(icon: ImageVector, label: String, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = Modifier.size(60.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
    }
}
