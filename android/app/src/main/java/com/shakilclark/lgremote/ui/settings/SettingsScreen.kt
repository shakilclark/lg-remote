package com.shakilclark.lgremote.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.components.MaterialSymbols
import com.shakilclark.lgremote.ui.components.SymbolIcon
import com.shakilclark.lgremote.ui.theme.Space

/**
 * App settings (spec 024) — a dedicated destination with a large collapsing top app bar and M3
 * Expressive "segmented" grouped sections: TV/Connection · Appearance · Behaviour · About. Distinct
 * from the TV's own settings (the command-sheet gear). Appearance navigates to the theme picker (021).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tvName: String,
    tvAddress: String?,
    themeSummary: String,
    hapticsEnabled: Boolean,
    versionLabel: String,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    onRepair: () -> Unit,
    onForget: () -> Unit,
    onOpenAppearance: () -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onResetHints: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmForget by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButtonRow(onBack)
                },
                scrollBehavior = scroll,
            )
        },
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = Space.l, end = Space.l, bottom = Space.xxl,
                top = inner.calculateTopPadding(),
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Space.l),
        ) {
            item {
                Group("TV / Connection") {
                    InfoRow(MaterialSymbols.Tv, tvName.ifEmpty { "TV" }, tvAddress ?: "Not connected")
                    NavRow(MaterialSymbols.Sync, "Re-scan for TVs", onClick = onRescan, chevron = false)
                    NavRow(MaterialSymbols.Tv, "Re-pair", onClick = onRepair, chevron = false)
                    NavRow(
                        MaterialSymbols.PowerSettingsNew, "Forget this TV",
                        onClick = { confirmForget = true }, chevron = false, destructive = true,
                    )
                }
            }
            item {
                Group("Appearance") {
                    NavRow(MaterialSymbols.Palette, "Theme & colours", supporting = themeSummary, onClick = onOpenAppearance)
                }
            }
            item {
                Group("Behaviour") {
                    SwitchRow(MaterialSymbols.Tune, "Haptic feedback", hapticsEnabled, onToggleHaptics)
                    NavRow(MaterialSymbols.Sync, "Reset first-run hints", onClick = onResetHints, chevron = false)
                }
            }
            item {
                Group("About") {
                    InfoRow(MaterialSymbols.Info, "Version", versionLabel)
                    NavRow(MaterialSymbols.Description, "Open-source licences", onClick = onOpenLicenses)
                }
            }
        }
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text("Forget this TV?") },
            text = { Text("The remote will disconnect and you'll need to pair again to use it.") },
            confirmButton = {
                TextButton(onClick = { confirmForget = false; onForget() }) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun IconButtonRow(onBack: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onBack) {
        SymbolIcon(MaterialSymbols.ArrowBack, contentDescription = "Back")
    }
}

/** A titled, tonal "segmented" group of rows (no per-row dividers). */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Space.l, bottom = Space.s, top = Space.s),
        )
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
            Column { content() }
        }
    }
}

private val rowColors
    @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun NavRow(
    symbol: String,
    title: String,
    onClick: () -> Unit,
    supporting: String? = null,
    chevron: Boolean = true,
    destructive: Boolean = false,
) {
    val content = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        leadingContent = { SymbolIcon(symbol, null, tint = content) },
        headlineContent = { Text(title, color = content) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = if (chevron) {
            { SymbolIcon(MaterialSymbols.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        colors = rowColors,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun SwitchRow(symbol: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        leadingContent = { SymbolIcon(symbol, null, tint = MaterialTheme.colorScheme.onSurface) },
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = rowColors,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun InfoRow(symbol: String, title: String, value: String) {
    ListItem(
        leadingContent = { SymbolIcon(symbol, null, tint = MaterialTheme.colorScheme.onSurface) },
        headlineContent = { Text(title) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = rowColors,
    )
}

/** Appearance sub-screen — placeholder until the theme picker (021) fills it in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SubScreen("Appearance", onBack, modifier) {
        Text(
            "Theme & colour options arrive with the theme picker.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Space.xl),
        )
    }
}

/** Open-source licences sub-screen — placeholder list home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SubScreen("Open-source licences", onBack, modifier) {
        Text(
            "Third-party licences will be listed here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Space.xl),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreen(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButtonRow(onBack) },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner)) { content() }
    }
}
