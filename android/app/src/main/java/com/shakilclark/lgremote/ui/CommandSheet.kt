package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space

/**
 * The pull-up command surface (010 — Direction C). One sheet hosts everything the gesture-pad home
 * doesn't: a segmented switch over **Keys** (the D-pad + Back/Home), **Apps**, and **Inputs**, with a
 * persistent Back/Home/Settings action row on top. Replaces the old per-feature sheets and the bottom
 * bar. (Keypad numbers/colour-keys and Sound/audio-output are separate later slices — they need new
 * SSAP commands — so they're intentionally not segments yet.)
 */
@Composable
fun CommandSheet(
    onNav: (NavButton) -> Unit,
    muted: Boolean = false,
    onToggleMute: () -> Unit = {},
    apps: List<TvApp>,
    onLaunchApp: (TvApp) -> Unit,
    inputs: List<TvInput>,
    onSelectInput: (TvInput) -> Unit,
    onOpenTvSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableIntStateOf(0) }
    val segments = listOf("Keys", "Apps", "Inputs")

    Column(
        modifier.fillMaxWidth().padding(horizontal = Space.l),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        // Persistent quick actions.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            QuickAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back") { onNav(NavButton.BACK) }
            QuickAction(Icons.Rounded.Home, "Home") { onNav(NavButton.HOME) }
            QuickAction(
                if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                if (muted) "Unmute" else "Mute",
                onClick = onToggleMute,
            )
            QuickAction(Icons.Rounded.Settings, "Settings", onClick = onOpenTvSettings)
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            segments.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = selected == i,
                    onClick = { selected = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = segments.size),
                ) { Text(label) }
            }
        }

        when (selected) {
            0 -> DirectionPad(onNav = onNav, modifier = Modifier.fillMaxWidth().padding(vertical = Space.s))
            1 -> AppGrid(apps = apps, onLaunch = onLaunchApp, modifier = Modifier.fillMaxWidth().height(360.dp))
            else -> InputSwitcher(
                inputs = inputs.filter { it.connected },
                onSelect = onSelectInput,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RowScope.QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    // Icon-only (label as contentDescription): a row of four labels doesn't fit horizontally.
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.height(20.dp))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CommandSheetPreview() {
    LGRemoteTheme {
        CommandSheet(
            onNav = {},
            apps = emptyList(),
            onLaunchApp = {},
            inputs = emptyList(),
            onSelectInput = {},
            onOpenTvSettings = {},
        )
    }
}
