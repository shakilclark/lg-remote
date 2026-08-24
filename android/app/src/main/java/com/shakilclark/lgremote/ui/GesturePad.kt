package com.shakilclark.lgremote.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.ui.components.MaterialSymbols
import com.shakilclark.lgremote.ui.components.SymbolIcon
import com.shakilclark.lgremote.ui.components.appHaptics
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.LocalReduceMotion
import com.shakilclark.lgremote.ui.theme.Space
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Finger travel → pointer travel gain (matches the prior touchpad feel). */
private const val GAIN = 1.6f

/**
 * Corner hit-box size (fraction of pad w/h) and the central OK radius. OK is the primary affordance,
 * so its target is generous (0.26 of the pad ≈ a disc wider than the drawn key); the four arrow wedges
 * only need a tap, so they keep the thinner ring left between this circle and the corners.
 */
private const val CORNER_X = 0.30f
private const val CORNER_Y = 0.24f
private const val OK_RADIUS = 0.26f

private enum class PadZone { Up, Down, Left, Right, Ok, Settings, Mute, Back, Home }

private fun zoneAt(x: Float, y: Float, w: Int, h: Int): PadZone {
    val nx = x / w
    val ny = y / h
    if (nx < CORNER_X && ny < CORNER_Y) return PadZone.Settings
    if (nx > 1 - CORNER_X && ny < CORNER_Y) return PadZone.Mute
    if (nx < CORNER_X && ny > 1 - CORNER_Y) return PadZone.Back
    if (nx > 1 - CORNER_X && ny > 1 - CORNER_Y) return PadZone.Home
    val dx = nx - 0.5f
    val dy = ny - 0.5f
    if (hypot(dx, dy) < OK_RADIUS) return PadZone.Ok
    val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
    return when {
        deg in -45.0..45.0 -> PadZone.Right
        deg in 45.0..135.0 -> PadZone.Down
        deg in -135.0..-45.0 -> PadZone.Up
        else -> PadZone.Left
    }
}

/**
 * The clickpad (spec 022 — Direction C). One flat tonal M3 surface that folds in cursor + D-pad: **glide**
 * moves the LG on-screen pointer; a **tap** dispatches by zone — the four edges navigate, the centre
 * is OK, and the four corners are actions (TL **Settings**, TR **Mute**, BL **Back**, BR **Home**).
 * Tap vs drag is one `awaitEachGesture` arbitrated by `touchSlop` (tap → zone, drag → cursor glide).
 * Volume is on the phone's hardware buttons (kept off the pad so it can't fight the Right tap-zone or
 * the cursor). Hints are faint at rest and surface on touch (reduce-motion aware); zones are exposed
 * as custom accessibility actions (incl. volume up/down).
 */
@Composable
fun GesturePad(
    onTouchStart: () -> Unit,
    onMove: (dx: Int, dy: Int) -> Unit,
    onClick: () -> Unit,
    onNav: (NavButton) -> Unit,
    onMute: () -> Unit,
    onOpenTvSettings: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    muted: Boolean = false,
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = appHaptics()
    val reduce = LocalReduceMotion.current
    var touched by remember { mutableStateOf(false) }
    var okPressed by remember { mutableStateOf(false) }

    fun tap(zone: PadZone) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        when (zone) {
            PadZone.Up -> onNav(NavButton.UP)
            PadZone.Down -> onNav(NavButton.DOWN)
            PadZone.Left -> onNav(NavButton.LEFT)
            PadZone.Right -> onNav(NavButton.RIGHT)
            PadZone.Ok -> onClick()
            PadZone.Settings -> onOpenTvSettings()
            PadZone.Mute -> onMute()
            PadZone.Back -> onNav(NavButton.BACK)
            PadZone.Home -> onNav(NavButton.HOME)
        }
    }

    Box(
        modifier
            // M3 Expressive: a flat tonal surface (depth via tonal colour, not shadows/skeuomorphism).
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceContainerHigh)
            .semantics {
                contentDescription = "Touchpad — glide to move the pointer; tap edges to navigate, " +
                    "centre for OK; corners: Settings, Mute, Back, Home"
                customActions = listOf(
                    CustomAccessibilityAction("Up") { onNav(NavButton.UP); true },
                    CustomAccessibilityAction("Down") { onNav(NavButton.DOWN); true },
                    CustomAccessibilityAction("Left") { onNav(NavButton.LEFT); true },
                    CustomAccessibilityAction("Right") { onNav(NavButton.RIGHT); true },
                    CustomAccessibilityAction("OK") { onClick(); true },
                    CustomAccessibilityAction("Back") { onNav(NavButton.BACK); true },
                    CustomAccessibilityAction("Home") { onNav(NavButton.HOME); true },
                    CustomAccessibilityAction("Mute") { onMute(); true },
                    CustomAccessibilityAction("TV settings") { onOpenTvSettings(); true },
                    CustomAccessibilityAction("Volume up") { onVolumeUp(); true },
                    CustomAccessibilityAction("Volume down") { onVolumeDown(); true },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width
                    val h = size.height
                    // OK is its own button: a centre press must NOT engage the pad — no hint reveal,
                    // no cursor glide. Just press → release = click (with its own pressed state).
                    if (zoneAt(down.position.x, down.position.y, w, h) == PadZone.Ok) {
                        okPressed = true
                        val up = waitForUpOrCancellation()
                        okPressed = false
                        if (up != null) tap(PadZone.Ok)
                        return@awaitEachGesture
                    }
                    touched = true
                    val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (slop == null) {
                        // Never crossed slop → a tap. Fire the zone at the down position.
                        tap(zoneAt(down.position.x, down.position.y, w, h))
                    } else {
                        // Crossed slop → glide the pointer (volume lives on the hardware buttons).
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTouchStart()
                        var carryX = 0f
                        var carryY = 0f
                        drag(slop.id) { change ->
                            val d = change.positionChange()
                            carryX += d.x * GAIN
                            carryY += d.y * GAIN
                            val dx = carryX.roundToInt()
                            val dy = carryY.roundToInt()
                            if (dx != 0 || dy != 0) {
                                carryX -= dx; carryY -= dy
                                onMove(dx, dy)
                            }
                            change.consume()
                        }
                    }
                    touched = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        HintOverlay(touched = touched, reduceMotion = reduce, muted = muted)

        // Centre OK — the primary click affordance; its own button (presses don't engage the pad).
        OkButton(pressed = okPressed)

        if (showHint) {
            GestureHintCard(
                onDismiss = onDismissHint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(Space.m),
            )
        }
    }
}

/**
 * Recessed zone hints — edge chevrons (a rotated up-chevron), the four corner glyphs, and the volume
 * rail's affordance. Faint/embossed at rest; brighter on touch; the reveal respects reduce-motion.
 */
@Composable
private fun BoxScope.HintOverlay(touched: Boolean, reduceMotion: Boolean, muted: Boolean) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val alpha by animateFloatAsState(
        targetValue = if (touched) 0.55f else 0.14f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 160),
        label = "hintAlpha",
    )
    val pad = Space.m
    Chevron(0f, Modifier.align(Alignment.TopCenter).padding(top = pad), tint, alpha)
    Chevron(180f, Modifier.align(Alignment.BottomCenter).padding(bottom = pad), tint, alpha)
    Chevron(270f, Modifier.align(Alignment.CenterStart).padding(start = pad), tint, alpha)
    Chevron(90f, Modifier.align(Alignment.CenterEnd).padding(end = pad), tint, alpha)
    Corner(MaterialSymbols.Settings, Alignment.TopStart, pad, tint, alpha)
    // Reflects live TV state: crossed-out speaker when muted, speaker-with-waves when sound is on.
    Corner(if (muted) MaterialSymbols.VolumeOff else MaterialSymbols.VolumeUp, Alignment.TopEnd, pad, tint, alpha)
    Corner(MaterialSymbols.ArrowBack, Alignment.BottomStart, pad, tint, alpha)
    Corner(MaterialSymbols.Home, Alignment.BottomEnd, pad, tint, alpha)
}

/**
 * Centre OK — an M3 Expressive filled-tonal key: a flat `primaryContainer` disc (depth via tonal colour,
 * not shadow) carrying the high-emphasis click. On press it does the Expressive shape-morph — the circle
 * relaxes toward a rounded square and scales down a touch — then springs back. Colours come from the
 * scheme, so it follows dynamic / statement themes, light + dark.
 */
@Composable
private fun OkButton(pressed: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        if (pressed) 34.dp else 54.dp, animationSpec = tween(durationMillis = 150), label = "okCorner",
    )
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f, animationSpec = tween(durationMillis = 120), label = "okScale",
    )
    Box(
        Modifier
            .size(108.dp)
            .scale(scale)
            .clip(RoundedCornerShape(corner))
            .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "OK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun BoxScope.Chevron(rotation: Float, modifier: Modifier, tint: Color, alpha: Float) {
    SymbolIcon(
        MaterialSymbols.KeyboardArrowUp,
        contentDescription = null,
        tint = tint,
        size = 22.dp,
        modifier = modifier.alpha(alpha).rotate(rotation),
    )
}

@Composable
private fun BoxScope.Corner(
    symbol: String,
    alignment: Alignment,
    pad: androidx.compose.ui.unit.Dp,
    tint: Color,
    alpha: Float,
) {
    SymbolIcon(
        symbol,
        contentDescription = null,
        tint = tint,
        size = 22.dp,
        modifier = Modifier.align(alignment).padding(pad).alpha(alpha),
    )
}

/** One-time teaching card for the non-obvious gestures. */
@Composable
private fun GestureHintCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text("How the pad works", style = MaterialTheme.typography.titleMedium)
            Text(
                "Glide to move the pointer. Tap an edge to navigate, the centre for OK. Corners are " +
                    "Settings, Mute, Back, Home. Volume is on your phone's volume buttons.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Got it") }
        }
    }
}

@ThemePreviews
@Composable
private fun GesturePadPreview() {
    LGRemoteTheme {
        GesturePad(
            onTouchStart = {},
            onMove = { _, _ -> },
            onClick = {},
            onNav = {},
            onMute = {},
            onOpenTvSettings = {},
            onVolumeUp = {},
            onVolumeDown = {},
            showHint = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
