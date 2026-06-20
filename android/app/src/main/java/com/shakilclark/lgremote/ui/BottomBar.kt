package com.shakilclark.lgremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space

/**
 * The persistent bottom action bar (redesign). Order is frequency + grouping: Back · Home (navigate)
 * · Pad (point) · Apps · Inputs (content), then a gap and Settings (rare + disruptive, so kept away
 * from the frequent keys to avoid accidental launches). Each item is an icon with a small caption.
 */
@Composable
fun BottomBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenPad: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenInputs: () -> Unit,
    onOpenTvSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        BarItem(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        BarItem(Icons.Rounded.Home, "Home", onHome)
        BarItem(Icons.Rounded.TouchApp, "Pad", onOpenPad)
        BarItem(Icons.Rounded.Apps, "Apps", onOpenApps)
        BarItem(Icons.AutoMirrored.Rounded.Input, "Inputs", onOpenInputs)
        Spacer(Modifier.width(Space.l)) // separate rare/disruptive Settings from the frequent keys
        BarItem(Icons.Rounded.Settings, "Settings", onOpenTvSettings)
    }
}

@Composable
private fun RowScope.BarItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(vertical = Space.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BottomBarPreview() {
    LGRemoteTheme {
        BottomBar(
            onBack = {}, onHome = {}, onOpenPad = {}, onOpenApps = {}, onOpenInputs = {}, onOpenTvSettings = {},
        )
    }
}
