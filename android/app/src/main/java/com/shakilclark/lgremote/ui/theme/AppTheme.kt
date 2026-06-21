package com.shakilclark.lgremote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A user-selectable app theme (spec 021): **Dynamic** (Material You / wallpaper, Android 12+), the
 * fixed **Ultraviolet** identity, and the five statement themes. [scheme] returns the fixed light/dark
 * `ColorScheme` (null for Dynamic, which is resolved with a Context in [LGRemoteTheme]).
 */
enum class AppTheme(val label: String) {
    Dynamic("Dynamic"),
    Ultraviolet("Ultraviolet"),
    Obsidian("Obsidian Glass"),
    Wonka("Willy Wonka"),
    Vaporwave("Vaporwave"),
    Punk("Punk"),
    SimplyRed("Simply Red"),
    ;

    fun scheme(dark: Boolean): ColorScheme? = when (this) {
        Dynamic -> null
        Ultraviolet -> if (dark) UltravioletDark else UltravioletLight
        Obsidian -> if (dark) ObsidianDark else ObsidianLight
        Wonka -> if (dark) WonkaDark else WonkaLight
        Vaporwave -> if (dark) VaporwaveDark else VaporwaveLight
        Punk -> if (dark) PunkDark else PunkLight
        SimplyRed -> if (dark) SimplyRedDark else SimplyRedLight
    }

    /** Three representative chips (light scheme) for the picker swatch; empty for Dynamic. */
    val swatch: List<Color>
        get() = scheme(false)?.let { listOf(it.primary, it.secondary, it.tertiary) } ?: emptyList()

    companion object {
        fun fromId(id: String?): AppTheme = entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: Dynamic
    }
}

/** Decorative multi-hue gradient that represents Material You / dynamic colour on the picker card. */
val DynamicSwatchColors = listOf(
    Color(0xFF8AB4F8), Color(0xFFF28B82), Color(0xFFFDD663), Color(0xFF81C995),
)

enum class ThemeMode(val label: String) {
    Light("Light"),
    Dark("Dark"),
    System("System"),
    ;

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: System
    }
}
