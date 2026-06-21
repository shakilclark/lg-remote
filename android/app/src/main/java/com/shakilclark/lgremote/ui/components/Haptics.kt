package com.shakilclark.lgremote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * App-wide haptics on/off (Settings → Behaviour, spec 024). Provided in MainActivity from the
 * persisted preference. Call [appHaptics] instead of `LocalHapticFeedback.current` at button sites:
 * when disabled it returns a no-op so every `performHapticFeedback` becomes silent.
 */
val LocalHapticsEnabled = compositionLocalOf { true }

private val NoHaptics = object : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {}
}

@Composable
fun appHaptics(): HapticFeedback =
    if (LocalHapticsEnabled.current) LocalHapticFeedback.current else NoHaptics
