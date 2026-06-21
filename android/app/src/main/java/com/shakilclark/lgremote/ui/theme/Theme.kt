package com.shakilclark.lgremote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 010-expressive-redesign: Material 3 Expressive look, themed by Material You.
// The app follows the system light/dark setting and, on Android 12+, takes its colour from the
// wallpaper (dynamic colour). When dynamic colour is unavailable it falls back to the fixed
// Ultraviolet (#7B2FF7) identity below.
//
// Tones: the visible roles (primary/surface/containers/outline) are the device-validated design
// values from design-system §2.2/§2.3 — deliberately kept rather than re-toned. The remaining roles
// (error/inverse/surfaceDim/Bright/surfaceTint) are canonical values for the #7B2FF7 seed, generated
// with the Material Theme Builder algorithm — see tools/theme/ (`node tools/theme/gen.mjs`) to
// regenerate or verify the full scheme against the seed.

val UltravioletDark = darkColorScheme(
    primary = Color(0xFFD7BBFF),
    onPrimary = Color(0xFF46177D),
    primaryContainer = Color(0xFF5C2E9F),
    onPrimaryContainer = Color(0xFFEFDBFF),
    secondary = Color(0xFFCFC0E2),
    onSecondary = Color(0xFF352740),
    secondaryContainer = Color(0xFF4C3D63),
    onSecondaryContainer = Color(0xFFE9DDFB),
    tertiary = Color(0xFFF2B5D8),
    onTertiary = Color(0xFF4A1F38),
    tertiaryContainer = Color(0xFF5C3A4E),
    onTertiaryContainer = Color(0xFFFFD8EC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE8E0EE),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE8E0EE),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCDC4D6),
    surfaceContainerLowest = Color(0xFF0E0C12),
    surfaceContainerLow = Color(0xFF1C1922),
    surfaceContainer = Color(0xFF221E2A),
    surfaceContainerHigh = Color(0xFF2A2531),
    surfaceContainerHighest = Color(0xFF342D40),
    outline = Color(0xFF4C4456),
    outlineVariant = Color(0xFF332E3B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB),
    inverseSurface = Color(0xFFE6E1E6),
    inverseOnSurface = Color(0xFF323033),
    inversePrimary = Color(0xFF7423F0),
    surfaceDim = Color(0xFF141316),
    surfaceBright = Color(0xFF3B383C),
    surfaceTint = Color(0xFFD7BBFF),
    scrim = Color(0xFF000000),
)

val UltravioletLight = lightColorScheme(
    primary = Color(0xFF6E2EC9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFDBFF),
    onPrimaryContainer = Color(0xFF270056),
    secondary = Color(0xFF635269),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECDDFB),
    onSecondaryContainer = Color(0xFF211A2C),
    tertiary = Color(0xFF7C5267),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF301124),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE9DFEC),
    onSurfaceVariant = Color(0xFF4A454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6ECFC),
    surfaceContainer = Color(0xFFEFE3F8),
    surfaceContainerHigh = Color(0xFFE9DDF4),
    surfaceContainerHighest = Color(0xFFE3D7EE),
    outline = Color(0xFF7B757F),
    outlineVariant = Color(0xFFCCC4CF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF323033),
    inverseOnSurface = Color(0xFFF5EFF4),
    inversePrimary = Color(0xFFD2BBFF),
    surfaceDim = Color(0xFFDED8DD),
    surfaceBright = Color(0xFFFDF8FD),
    surfaceTint = Color(0xFF6E2EC9),
    scrim = Color(0xFF000000),
)

@Composable
fun LGRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You. Defaults OFF so previews/tests/goldens render the deterministic Ultraviolet
    // fallback; MainActivity opts the live app into dynamic colour (Android 12+).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> UltravioletDark
        else -> UltravioletLight
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(
            LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
            LocalReduceMotion provides systemReduceMotion(),
            content = content,
        )
    }
}
