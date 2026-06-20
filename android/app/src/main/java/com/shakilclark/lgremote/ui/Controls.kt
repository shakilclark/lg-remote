package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.components.ControlKey
import com.shakilclark.lgremote.ui.theme.Space

/** Volume up / down stack (US2). The level is shown as a caption in RemoteScreen. */
@Composable
fun VolumePad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.m)) {
        LabeledKey(Icons.Filled.Add, "Vol +", onUp, Modifier.fillMaxWidth().height(80.dp))
        LabeledKey(Icons.Filled.Remove, "Vol −", onDown, Modifier.fillMaxWidth().height(80.dp))
    }
}

/** Mute toggle + play/pause (US2). Mute turns to the error accent and desaturates the label. */
@Composable
fun PlaybackBar(
    muted: Boolean?,
    onToggleMute: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.m)) {
        LabeledKey(
            icon = if (muted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            label = if (muted == true) "Muted" else "Mute",
            onClick = onToggleMute,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            accent = muted == true,
        )
        LabeledKey(Icons.Filled.PlayArrow, "Play / Pause", onPlayPause, Modifier.fillMaxWidth().height(80.dp))
    }
}

/** A ControlKey carrying a centered icon + label, so the big transport keys share the morph/feel. */
@Composable
private fun LabeledKey(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    ControlKey(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(26.dp),
                tint = if (accent) MaterialTheme.colorScheme.error else LocalContentColor.current,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
