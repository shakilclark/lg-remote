package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.components.ControlKey
import com.shakilclark.lgremote.ui.theme.Space

/** Shared side for the centered control keys in the transport + volume rows. */
private val ControlCell = 64.dp

/**
 * Centered media transport row for the main remote: Rewind / Play-Pause / Fast-forward — the
 * SSAP-supported set. Pairs with [VolumeRow] as two consistent centered rows under the D-pad.
 * Play/pause is the existing blind toggle (webOS has no reliable play-state query).
 */
@Composable
fun TransportRow(
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onFastForward: () -> Unit,
    playing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ControlRow(modifier) {
        Key(Icons.Filled.FastRewind, "Rewind", onClick = onRewind)
        Key(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", onClick = onPlayPause)
        Key(Icons.Filled.FastForward, "Fast forward", onClick = onFastForward)
    }
}

/**
 * Centered volume row: Vol − / Mute / Vol +, icon-only. Mute turns to the error accent while muted.
 * The phone's hardware volume rocker drives the TV too.
 */
@Composable
fun VolumeRow(
    onUp: () -> Unit,
    onDown: () -> Unit,
    muted: Boolean?,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ControlRow(modifier) {
        Key(Icons.Filled.Remove, "Volume down", onClick = onDown)
        Key(
            icon = if (muted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            label = "Mute",
            accent = muted == true,
            onClick = onToggleMute,
        )
        Key(Icons.Filled.Add, "Volume up", onClick = onUp)
    }
}

@Composable
private fun ControlRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.xl, Alignment.CenterHorizontally),
    ) { content() }
}

@Composable
private fun Key(icon: ImageVector, label: String, accent: Boolean = false, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = Modifier.size(ControlCell)) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = if (accent) MaterialTheme.colorScheme.error else LocalContentColor.current,
        )
    }
}
