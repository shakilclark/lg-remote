package com.shakilclark.lgremote.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the user has asked the system to remove animations (Accessibility "Remove animations" — a
 * zero animator-duration scale). Gate spatial motion on this (design-system §6/§10): drop springs,
 * pulses and shape morphs to instant state changes; colour/alpha "effects" may stay. Provided by
 * [LGRemoteTheme]; defaults false (e.g. in previews/goldens).
 */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun systemReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
