package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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

/** Shared side for the centered control keys in the volume row. */
private val ControlCell = 64.dp

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
        Key(Icons.Rounded.Remove, "Volume down", onClick = onDown)
        Key(
            icon = if (muted == true) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            label = "Mute",
            accent = muted == true,
            onClick = onToggleMute,
        )
        Key(Icons.Rounded.Add, "Volume up", onClick = onUp)
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
