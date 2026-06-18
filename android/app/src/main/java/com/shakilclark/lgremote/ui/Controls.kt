package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.ui.theme.AccentSoft
import com.shakilclark.lgremote.ui.theme.Muted

/** Volume up / down stack (US2). The level is shown as a caption in RemoteScreen. */
@Composable
fun VolumePad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ControlButton(icon = Icons.Filled.Add, label = "Vol +", onClick = onUp, modifier = Modifier.fillMaxWidth().height(84.dp))
        ControlButton(icon = Icons.Filled.Remove, label = "Vol −", onClick = onDown, modifier = Modifier.fillMaxWidth().height(84.dp))
    }
}

/** Mute + play/pause (US2). */
@Composable
fun PlaybackBar(
    muted: Boolean?,
    onToggleMute: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ControlButton(
            icon = if (muted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            label = if (muted == true) "Muted" else "Mute",
            onClick = onToggleMute,
            tint = if (muted == true) AccentSoft else null,
            modifier = Modifier.fillMaxWidth().height(84.dp),
        )
        ControlButton(
            icon = Icons.Filled.PlayArrow,
            label = "Play / Pause",
            onClick = onPlayPause,
            modifier = Modifier.fillMaxWidth().height(84.dp),
        )
    }
}

/** A large labelled control button with press haptics (FR-012). */
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp), tint = tint ?: MaterialTheme.colorScheme.onSurface)
            Text(label, color = tint ?: Muted, fontSize = 12.sp)
        }
    }
}
