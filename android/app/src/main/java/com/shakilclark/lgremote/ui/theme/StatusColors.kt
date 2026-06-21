package com.shakilclark.lgremote.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic connection/state colours (design-system §2.4) — these are NOT M3 `ColorScheme` roles, they
 * are state colours tuned per light/dark mode. Always pair with an icon/label; never rely on colour
 * alone. Provided via [LocalStatusColors] in [LGRemoteTheme]; off-network uses `colorScheme.outline`
 * and error states use `colorScheme.error`, so only success/warn live here.
 */
data class StatusColors(
    val success: Color,
    val warn: Color,
)

val LightStatusColors = StatusColors(success = Color(0xFF2E7D4F), warn = Color(0xFF8A6500))
val DarkStatusColors = StatusColors(success = Color(0xFF7FD49B), warn = Color(0xFFF2C14E))

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
