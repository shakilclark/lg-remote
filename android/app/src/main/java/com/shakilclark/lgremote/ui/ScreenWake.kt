package com.shakilclark.lgremote.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** How long the remote sits idle before the screen dims, and how dim it goes (0..1). */
private const val IDLE_BEFORE_DIM_MS = 2_000L // TODO: restore to ~12s after testing
private const val DIM_BRIGHTNESS = 0.02f

/** A hot signal pulsed on every user interaction; resets the always-on dim timer. */
@Composable
fun rememberInteractionSignal(): MutableSharedFlow<Unit> =
    remember { MutableSharedFlow(extraBufferCapacity = 1) }

/**
 * Always-on mode (spec 020 polish). While [enabled] and the remote is on screen, hold the screen awake
 * (`FLAG_KEEP_SCREEN_ON`) so the user never has to re-wake/unlock the phone just to nudge the TV — and
 * after [idleMillis] of no [interactions], drop the *window* brightness to [dimLevel] (a soft always-on-
 * display feel) so it isn't burning full brightness. The next interaction restores brightness instantly.
 *
 * The dim timer is driven off the [interactions] flow (not Compose state), so the high-frequency pointer
 * events during a cursor glide reset it without recomposing the UI. Clears the flag and brightness
 * override on dispose / when disabled.
 */
@Composable
fun ScreenWakeEffect(
    enabled: Boolean,
    interactions: SharedFlow<Unit>,
    idleMillis: Long = IDLE_BEFORE_DIM_MS,
    dimLevel: Float = DIM_BRIGHTNESS,
) {
    val window = LocalView.current.context.findActivity()?.window

    DisposableEffect(window, enabled) {
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    LaunchedEffect(window, enabled) {
        if (window == null || !enabled) {
            window?.setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
            return@LaunchedEffect
        }
        while (true) {
            window.setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
            // Wait out the idle window; an interaction in that time restarts the cycle at full brightness.
            val interacted = withTimeoutOrNull(idleMillis) { interactions.first() } != null
            if (!interacted) {
                window.setBrightness(dimLevel)
                interactions.first() // stay dim until the next touch, then loop back to full brightness
            }
        }
    }
}

/**
 * Pulse [onInteraction] on every pointer down, without consuming the event, so the touch still reaches
 * the controls underneath. Apply to a container wrapping the remote UI; pass `{ signal.tryEmit(Unit) }`.
 */
fun Modifier.trackInteractions(onInteraction: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            // Initial pass + no consume: we only observe; children still receive the event.
            awaitPointerEvent(PointerEventPass.Initial)
            onInteraction()
        }
    }
}

private fun Window.setBrightness(value: Float) {
    attributes = attributes.apply { screenBrightness = value }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
