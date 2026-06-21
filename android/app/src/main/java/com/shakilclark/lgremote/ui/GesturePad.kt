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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
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

/** Vertical travel (px) that emits one volume step on the edge rail. */
private const val VOL_STEP_PX = 44f

/** Width of the drag-only volume rail. */
private val RAIL_WIDTH = 40.dp

/** Corner hit-box size (fraction of pad w/h); central OK dead-zone radius; rail's vertical band. */
private const val CORNER_X = 0.30f
private const val CORNER_Y = 0.24f
private const val OK_RADIUS = 0.16f
private const val RAIL_TOP = 0.26f
private const val RAIL_BOTTOM = 0.74f

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
 * is OK, and the four corners are actions (TL **Settings**, TR **Mute**, BL **Back**, BR **Home**). A
 * slim **drag-only volume rail** runs down the right edge between the corners (volume is also on the
 * phone's hardware buttons). Tap vs drag is one `awaitEachGesture` arbitrated by `touchSlop`; drags
 * are routed by start zone (rail → volume, elsewhere → cursor) so volume never fights the cursor.
 * Hints are recessed at rest and surface on touch (reduce-motion aware); zones are exposed as custom
 * accessibility actions.
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
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = appHaptics()
    val reduce = LocalReduceMotion.current
    var touched by remember { mutableStateOf(false) }

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
            .recessedWell(scheme.surfaceContainer, scheme.surfaceContainerLowest, scheme.onSurface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(26.dp))
            .semantics {
                contentDescription = "Touchpad — glide to move the pointer; tap edges to navigate, " +
                    "centre for OK; corners: Settings, Mute, Back, Home; right edge drags volume"
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
                val railPx = RAIL_WIDTH.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touched = true
                    val w = size.width
                    val h = size.height
                    val inRail = down.position.x > w - railPx &&
                        down.position.y in (h * RAIL_TOP)..(h * RAIL_BOTTOM)
                    if (inRail) {
                        // Drag-only volume rail — immediate stepped vertical tracking (no slop).
                        var acc = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            acc += change.positionChange().y
                            change.consume()
                            while (acc <= -VOL_STEP_PX) {
                                acc += VOL_STEP_PX
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onVolumeUp()
                            }
                            while (acc >= VOL_STEP_PX) {
                                acc -= VOL_STEP_PX
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onVolumeDown()
                            }
                        }
                    } else {
                        val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        if (slop == null) {
                            // Never crossed slop → a tap. Fire the zone at the down position.
                            tap(zoneAt(down.position.x, down.position.y, w, h))
                        } else {
                            // Crossed slop → glide the pointer.
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
                    }
                    touched = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        HintOverlay(touched = touched, reduceMotion = reduce)

        // Centre OK ring — the primary click affordance.
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "OK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary,
            )
        }

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
private fun BoxScope.HintOverlay(touched: Boolean, reduceMotion: Boolean) {
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
    Corner(MaterialSymbols.Settings, "Settings", Alignment.TopStart, pad, tint, alpha)
    Corner(MaterialSymbols.VolumeOff, "Mute", Alignment.TopEnd, pad, tint, alpha)
    Corner(MaterialSymbols.ArrowBack, "Back", Alignment.BottomStart, pad, tint, alpha)
    Corner(MaterialSymbols.Home, "Home", Alignment.BottomEnd, pad, tint, alpha)
    // Volume rail affordance — a slim dashed strip between the right corners.
    Box(
        Modifier.align(Alignment.CenterEnd).fillMaxHeight(RAIL_BOTTOM - RAIL_TOP).width(RAIL_WIDTH)
            .padding(end = 2.dp).alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.l)) {
            SymbolIcon(MaterialSymbols.KeyboardArrowUp, contentDescription = null, tint = tint, size = 16.dp)
            Text("VOL", style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.rotate(-90f))
            SymbolIcon(MaterialSymbols.KeyboardArrowDown, contentDescription = null, tint = tint, size = 16.dp)
        }
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
    label: String,
    alignment: Alignment,
    pad: androidx.compose.ui.unit.Dp,
    tint: Color,
    alpha: Float,
) {
    Column(
        Modifier.align(alignment).padding(pad).alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SymbolIcon(symbol, contentDescription = null, tint = tint, size = 20.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/**
 * The pad's recessed, textured surface (design-system §5.2): a concave radial (lighter centre →
 * darker edge), a faint 7dp dot texture, and an inset top shadow + highlight so it reads as a well.
 */
private fun Modifier.recessedWell(surface: Color, surfaceLow: Color, onSurface: Color): Modifier =
    drawWithCache {
        val tilePx = 7.dp.toPx().toInt().coerceAtLeast(2)
        val r = 0.6.dp.toPx()
        val tile = ImageBitmap(tilePx, tilePx)
        Canvas(tile).drawCircle(Offset(r, r), r, Paint().apply { color = onSurface.copy(alpha = 0.06f) })
        val dots = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
        val base = Brush.radialGradient(
            0f to surfaceLow,
            0.58f to surface,
            1f to lerp(surface, Color.Black, 0.08f),
            center = Offset(size.width * 0.5f, size.height * 0.36f),
            radius = size.maxDimension * 0.95f,
        )
        val top = 18.dp.toPx()
        val topShadow = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.16f), 1f to Color.Transparent, startY = 0f, endY = top,
        )
        onDrawBehind {
            drawRect(base)
            drawRect(dots)
            drawRect(topShadow, size = Size(size.width, top))
            drawRect(Color.White.copy(alpha = 0.35f), size = Size(size.width, 1.dp.toPx()))
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
                    "Settings, Mute, Back, Home; drag the right edge for volume.",
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
