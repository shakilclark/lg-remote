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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
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
    PlayState.Unknown -> "" // TV doesn't report it (e.g. Netflix) — show the app name only
}

/**
 * Persistent now-playing bar pinned to the top whenever something is on (004 redesign): app icon +
 * name (tap to expand the cover sheet) with the full transport inline — rewind / play-pause /
 * fast-forward / stop. The play-pause glyph reflects [NowPlaying.playState] (optimistic when the TV
 * doesn't report it). This is the single home for media transport — there's no separate row.
 */
@Composable
fun NowPlayingBar(
    nowPlaying: NowPlaying,
    onExpand: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onFastForward: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loader = rememberTvImageLoader()
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Space.s, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExpand()
                }
                .semantics { contentDescription = "Now playing: ${nowPlaying.name}" }
                .padding(Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Box(
                Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(nowPlaying.iconUrl, nowPlaying.name, loader, Modifier.fillMaxSize().padding(Space.xs))
            }
            Text(
                nowPlaying.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BarKey(Icons.Filled.FastRewind, "Rewind", onRewind)
        BarKey(if (nowPlaying.playState == PlayState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", onPlayPause)
        BarKey(Icons.Filled.FastForward, "Fast forward", onFastForward)
        BarKey(Icons.Filled.Stop, "Stop", onStop)
    }
}

@Composable
private fun BarKey(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
    }
}

/**
 * Expanded now-playing as a cover view (004 redesign + "cover if available"): a large app-icon
 * cover, the app name + play-state, and the full transport. webOS exposes no real artwork for app
 * playback, so the app icon is the cover; real art would slot in here if a source ever provided it.
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
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Box(
            Modifier.size(132.dp).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(nowPlaying.iconUrl, nowPlaying.name, loader, Modifier.fillMaxSize().padding(Space.xl))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(
                nowPlaying.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            val label = stateLabel(nowPlaying.playState)
            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.l, Alignment.CenterHorizontally)) {
            SheetKey(Icons.Filled.FastRewind, "Rewind", onRewind)
            SheetKey(if (nowPlaying.playState == PlayState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", onPlayPause)
            SheetKey(Icons.Filled.FastForward, "Fast forward", onFastForward)
            SheetKey(Icons.Filled.Stop, "Stop", onStop)
        }
    }
}

@Composable
private fun SheetKey(icon: ImageVector, label: String, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = Modifier.size(60.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
    }
}
