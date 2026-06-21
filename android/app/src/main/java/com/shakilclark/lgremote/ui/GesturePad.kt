package com.shakilclark.lgremote.ui

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
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

/** Corner hit-box size (fraction of pad w/h) and the central OK dead-zone radius. */
private const val CORNER_X = 0.30f
private const val CORNER_Y = 0.24f
private const val OK_RADIUS = 0.16f

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
 * The clickpad (spec 022 — Direction C). One recessed surface that folds in cursor + D-pad: **glide**
 * moves the LG on-screen pointer; a **tap** dispatches by zone — the four edges navigate, the centre
 * is OK, and the four corners are actions (TL **Settings**, TR **Mute**, BL **Back**, BR **Home**).
 * Tap vs drag is one `awaitEachGesture` arbitrated by `touchSlop` (tap → zone, drag → cursor glide).
 * Volume is on the phone's hardware buttons (kept off the pad so it can't fight the Right tap-zone or
 * the cursor). Hints are recessed at rest and surface on touch (reduce-motion aware); zones are exposed
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
            .clip(RoundedCornerShape(26.dp))
            .recessedWell(scheme.surfaceContainer, scheme.onSurface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(26.dp))
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
 * Centre OK — a **recessed rubber** key (chosen in the design workshop). No drop shadow, no bright top:
 * a matte disc dialled slightly darker than the pad, with a soft inset top shadow + a faint bottom lip,
 * so it reads as a dip pressed into the rubber rather than a raised dome. Tones derive from the scheme
 * (surfaceContainerHigh nudged toward black), so it follows the dynamic / statement themes, light + dark.
 */
@Composable
private fun OkButton(pressed: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    // Matches web target A: a light, low-contrast lilac disc dialled just below the pad, with a soft
    // inset top shadow (purple-tinted, like the web's rgba(70,40,110)) and a faint bright bottom lip —
    // gentle depth, not a dark bowl. Mode-aware. On press it sinks: the top shadow deepens, fill darkens
    // a touch, the lip dims (subtle, animated).
    val press by animateFloatAsState(
        if (pressed) 1f else 0f, animationSpec = tween(durationMillis = 90), label = "okPress",
    )
    val fill = lerp(scheme.surfaceContainerHigh, Color.Black, (if (dark) 0.12f else 0.045f) + press * 0.05f)
    val crestTint = lerp(Color.Black, scheme.primary, 0.20f) // soft purple-black, like the web shadow
    val crestA = (if (dark) 0.30f else 0.20f) + press * 0.16f
    val lipA = (if (dark) 0.03f else 0.15f) * (1f - press * 0.4f)
    Box(
        Modifier
            .size(80.dp)
            .clip(CircleShape)
            .drawWithCache {
                val crest = Brush.verticalGradient(
                    0f to crestTint.copy(alpha = crestA), 1f to Color.Transparent,
                    startY = 0f, endY = size.height * 0.22f,
                )
                val lip = Brush.verticalGradient(
                    0f to Color.Transparent, 1f to Color.White.copy(alpha = lipA),
                    startY = size.height * 0.90f, endY = size.height,
                )
                onDrawBehind {
                    drawRect(fill)
                    drawRect(crest)
                    drawRect(lip)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "OK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurfaceVariant,
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

/**
 * The pad's recessed surface (design-system §5.2) — a **directionally-lit inset tray**, not a bowl: a
 * flat, even field (a centred radial reads as a bulging sphere on-device), with depth coming from a soft
 * shadow down the top lip + side walls and a lit bottom lip — the cue your eye reads as "sunken panel".
 * A **cross-hatch mesh** (woven 45°/-45° lines) gives it a rubbery tactility — drawn with exact geometry
 * (6dp perpendicular spacing, 1dp lines, matching the web ref's 1px/6px hatch) into a bitmap that's
 * cached by [drawWithCache] and rebuilt only when the size changes, so dragging stays cheap.
 */
private fun Modifier.recessedWell(surface: Color, onSurface: Color): Modifier =
    drawWithCache {
        val mw = size.width.toInt().coerceAtLeast(1)
        val mh = size.height.toInt().coerceAtLeast(1)
        val meshBmp = ImageBitmap(mw, mh)
        val meshCanvas = Canvas(meshBmp)
        val meshPaint = Paint().apply {
            color = onSurface.copy(alpha = 0.05f) // matches the web ref hatch alpha
            strokeWidth = 1.dp.toPx() // density-correct ~1px line
            isAntiAlias = true
        }
        val fh = mh.toFloat()
        val step = 6.dp.toPx() * 1.41421f // intercept step for 6dp perpendicular line spacing
        var k = -fh
        while (k <= mw) { // "\" diagonals (slope +1)
            meshCanvas.drawLine(Offset(k, 0f), Offset(k + fh, fh), meshPaint); k += step
        }
        k = 0f
        while (k <= mw + fh) { // "/" diagonals (slope -1)
            meshCanvas.drawLine(Offset(k, 0f), Offset(k - fh, fh), meshPaint); k += step
        }
        // Flat field: the overhang shades the very top, then it settles to an even surface. No radial.
        val field = Brush.verticalGradient(
            0f to lerp(surface, Color.Black, 0.05f), 0.16f to surface, 1f to surface,
        )
        val topPx = 16.dp.toPx()
        val sidePx = 12.dp.toPx()
        val lipPx = 5.dp.toPx()
        val topShadow = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.20f), 1f to Color.Transparent, startY = 0f, endY = topPx,
        )
        val leftShadow = Brush.horizontalGradient(
            0f to Color.Black.copy(alpha = 0.08f), 1f to Color.Transparent, startX = 0f, endX = sidePx,
        )
        val rightShadow = Brush.horizontalGradient(
            0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.08f),
            startX = size.width - sidePx, endX = size.width,
        )
        val bottomLip = Brush.verticalGradient(
            0f to Color.Transparent, 1f to Color.White.copy(alpha = 0.28f),
            startY = size.height - lipPx, endY = size.height,
        )
        onDrawBehind {
            drawRect(field)
            drawImage(meshBmp)
            drawRect(topShadow, size = Size(size.width, topPx))
            drawRect(leftShadow, size = Size(sidePx, size.height))
            drawRect(rightShadow, topLeft = Offset(size.width - sidePx, 0f), size = Size(sidePx, size.height))
            drawRect(bottomLip, topLeft = Offset(0f, size.height - lipPx), size = Size(size.width, lipPx))
        }
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
