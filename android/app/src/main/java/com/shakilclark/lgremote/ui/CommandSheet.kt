package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.ui.components.MaterialSymbols
import com.shakilclark.lgremote.ui.components.SymbolIcon
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space

/**
 * The pull-up command surface (010 — direction-c.html). A persistent Back/Home/Mute/Settings quick
 * row over a segmented switch: **Keypad · Apps · Inputs · Sound · Type**. Apps and Inputs are live;
 * Keypad, Sound and Type are **inactive shells** pending their features (see spec 020) and render a
 * disabled layout with an honest "coming soon" note — no fake controls.
 */
@Composable
fun CommandSheet(
    onNav: (NavButton) -> Unit,
    muted: Boolean = false,
    onToggleMute: () -> Unit = {},
    apps: List<TvApp>,
    appsLoading: Boolean = false,
    onLaunchApp: (TvApp) -> Unit,
    inputs: List<TvInput>,
    onSelectInput: (TvInput) -> Unit,
    onOpenTvSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableIntStateOf(1) } // default to Apps (a live segment)
    val segments = listOf("Keypad", "Apps", "Inputs", "Sound", "Type")

    Column(
        modifier.fillMaxWidth().padding(horizontal = Space.l),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            QuickAction(MaterialSymbols.ArrowBack, "Back") { onNav(NavButton.BACK) }
            QuickAction(MaterialSymbols.Home, "Home") { onNav(NavButton.HOME) }
            QuickAction(
                if (muted) MaterialSymbols.VolumeOff else MaterialSymbols.VolumeUp,
                if (muted) "Unmute" else "Mute",
                filled = muted,
                onClick = onToggleMute,
            )
            QuickAction(MaterialSymbols.Settings, "Settings", onClick = onOpenTvSettings)
        }

        PillSegmented(
            segments = segments,
            selectedIndex = selected,
            onSelect = { selected = it },
        )

        when (selected) {
            0 -> ComingSoon("Number pad", "Channel & PIN entry is coming soon.")
            1 -> AppGrid(apps = apps, loading = appsLoading, onLaunch = onLaunchApp, modifier = Modifier.fillMaxWidth().height(360.dp))
            2 -> InputSwitcher(
                inputs = inputs.filter { it.connected },
                onSelect = onSelectInput,
                modifier = Modifier.fillMaxWidth(),
            )
            3 -> ComingSoon("Audio output", "Switch TV speakers / soundbar / Bluetooth — coming soon.")
            else -> ComingSoon("Keyboard", "On-screen text entry is coming soon.")
        }
    }
}

/**
 * Filled-pill segmented switch (010 — direction-c.html). A fully-rounded tinted track; the selected
 * segment is a `primary`-filled pill with `onPrimary` text — no outline, divider or checkmark. Colours
 * come from the Material You scheme (Ultraviolet is only the dynamic-off fallback seed).
 */
@Composable
private fun PillSegmented(
    segments: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier
            .fillMaxWidth()
            .clip(pill)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEachIndexed { i, label ->
            val isSelected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(pill)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(role = Role.Button) { onSelect(i) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RowScope.QuickAction(symbol: String, label: String, filled: Boolean = false, onClick: () -> Unit) {
    // Icon-only (label as contentDescription): a row of four labels doesn't fit horizontally.
    FilledTonalButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        SymbolIcon(symbol, contentDescription = label, size = 20.dp, filled = filled)
    }
}

/** Inactive-shell placeholder for a segment whose feature isn't wired yet (spec 020). */
@Composable
private fun ComingSoon(title: String, detail: String) {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Space.xl),
            )
        }
    }
}

@ThemePreviews
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