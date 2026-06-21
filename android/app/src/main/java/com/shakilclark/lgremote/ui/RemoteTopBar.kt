package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.LocalStatusColors
import com.shakilclark.lgremote.ui.theme.Space

/**
 * Home top bar (010 — direction-c.html appbar): a connection chip (TV name + live dot), a voice
 * button, and a power button. Voice is an inactive shell pending its feature (020); power is wired
 * to system/turnOff.
 */
@Composable
fun RemoteTopBar(
    tvName: String,
    onPowerOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.weight(1f),
        ) {
            Row(
                Modifier.padding(horizontal = Space.m, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(LocalStatusColors.current.success))
                Text(
                    tvName.ifEmpty { "TV" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(Space.s))
        // Voice — inactive shell (020): visibly disabled until the feature lands.
        FilledTonalIconButton(onClick = {}, enabled = false) {
            Icon(Icons.Rounded.Mic, contentDescription = "Voice (coming soon)")
        }
        Spacer(Modifier.width(Space.xs))
        FilledTonalIconButton(onClick = onPowerOff) {
            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Power off")
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RemoteTopBarPreview() {
    LGRemoteTheme {
        RemoteTopBar(tvName = "Living Room TV", onPowerOff = {}, modifier = Modifier.padding(8.dp))
    }
}
