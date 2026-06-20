package com.shakilclark.lgremote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Full M3 role mapping over the electric-red dark brand (design-system §2.2), so every component
// inherits the brand instead of falling back to Material purple. Dynamic color is intentionally off.
private val DarkColors = darkColorScheme(
    primary = AccentSoft, // #E23A5E — main interactive accent
    onPrimary = Color(0xFF1A0309),
    primaryContainer = Color(0xFF7A0C25), // OK btn, live chip
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFB9C0CF),
    onSecondary = Color(0xFF1A1D24),
    secondaryContainer = Panel2,
    onSecondaryContainer = TextPrimary,
    tertiary = Ok, // connected accent
    onTertiary = Color(0xFF06170E),
    background = Bg,
    onBackground = TextPrimary,
    surface = Bg,
    onSurface = TextPrimary,
    surfaceContainerLowest = Color(0xFF0A0B0E),
    surfaceContainerLow = Panel, // #181A21
    surfaceContainer = Color(0xFF1C1F27),
    surfaceContainerHigh = Panel2, // #1F222B — resting control fill
    surfaceContainerHighest = Color(0xFF262A34),
    surfaceVariant = Panel2,
    onSurfaceVariant = Muted,
    outline = Edge, // #2B2F3A
    outlineVariant = Color(0xFF22252E),
    error = AccentSoft,
    onError = Color(0xFF1A0309),
    scrim = Color(0xCC000000),
)

@Composable
fun LGRemoteTheme(
    // The remote is dark-first regardless of system setting; param kept for previews.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
