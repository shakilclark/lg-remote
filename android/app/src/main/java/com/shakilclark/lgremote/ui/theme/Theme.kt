package com.shakilclark.lgremote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentSoft,
    onPrimary = TextPrimary,
    secondary = Accent,
    background = Bg,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = Panel2,
    onSurfaceVariant = Muted,
    outline = Edge,
    error = AccentSoft,
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
        content = content,
    )
}
