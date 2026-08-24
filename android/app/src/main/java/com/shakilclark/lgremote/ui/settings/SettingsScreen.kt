package com.shakilclark.lgremote.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.components.MaterialSymbols
import com.shakilclark.lgremote.ui.components.SymbolIcon
import com.shakilclark.lgremote.ui.theme.AppTheme
import com.shakilclark.lgremote.ui.theme.DynamicSwatchColors
import com.shakilclark.lgremote.ui.theme.ThemeMode
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
    alwaysOn: Boolean,
    versionLabel: String,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    onRepair: () -> Unit,
    onForget: () -> Unit,
    onOpenAppearance: () -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleAlwaysOn: (Boolean) -> Unit,
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
                    SwitchRow(
                        MaterialSymbols.LightMode, "Always-on remote", alwaysOn, onToggleAlwaysOn,
                        supporting = "Show on the lock screen, keep the screen on while connected, and dim when idle",
                    )
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
private fun SwitchRow(
    symbol: String,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    ListItem(
        leadingContent = { SymbolIcon(symbol, null, tint = MaterialTheme.colorScheme.onSurface) },
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
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

/**
 * Appearance / theme picker (spec 021): a Light/Dark/System segmented control over a gallery of
 * theme swatch cards (Dynamic + Ultraviolet + the statement themes). Selecting a card re-themes the
 * whole app instantly (the choice is persisted and drives [LGRemoteTheme] at the root).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    current: AppTheme,
    mode: ThemeMode,
    dynamicAvailable: Boolean,
    onSelectTheme: (AppTheme) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = AppTheme.entries.filter { it != AppTheme.Dynamic || dynamicAvailable }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Appearance") }, navigationIcon = { IconButtonRow(onBack) }) },
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = Space.l, end = Space.l, bottom = Space.xxl, top = inner.calculateTopPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = Space.s)) {
                    ThemeMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = m == mode,
                            onClick = { onSelectMode(m) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        ) { Text(m.label) }
                    }
                }
            }
            items(themes) { theme ->
                ThemeCard(theme, selected = theme == current) { onSelectTheme(theme) }
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.semantics { this.selected = selected; role = Role.RadioButton },
    ) {
        Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Swatch(theme)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(theme.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (selected) {
                    SymbolIcon(MaterialSymbols.Check, contentDescription = "Selected", tint = scheme.primary, size = 18.dp)
                }
            }
        }
    }
}

@Composable
private fun Swatch(theme: AppTheme) {
    val shape = MaterialTheme.shapes.small
    if (theme == AppTheme.Dynamic) {
        Box(
            Modifier.fillMaxWidth().height(30.dp).clip(shape)
                .background(Brush.horizontalGradient(DynamicSwatchColors)),
        )
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            theme.swatch.forEach { c ->
                Box(Modifier.weight(1f).height(30.dp).clip(shape).background(c))
            }
        }
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
