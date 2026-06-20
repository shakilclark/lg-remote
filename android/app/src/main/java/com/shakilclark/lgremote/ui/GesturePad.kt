package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space
import kotlin.math.roundToInt

/** Finger travel → pointer travel gain (matches the prior touchpad feel). */
private const val GAIN = 1.6f

/** Width of the volume/channel edge rockers. */
private val EDGE_WIDTH = 64.dp

/** Vertical travel that emits one volume/channel step when dragging an edge. */
private const val STEP_PX = 44f

/**
 * Recessed gesture pad (010 — Direction C). The centre drives the LG on-screen pointer (glide to
 * move, tap to click — the same pointer socket the D-pad uses); the **right** edge is a volume
 * rocker and the **left** edge a channel rocker (drag up/down for steps, or tap the upper/lower
 * half). The edges carry a faint always-on affordance so they read as interactive, and [showHint]
 * raises a one-time teaching card for the two edge gestures.
 */
@Composable
fun GesturePad(
    onTouchStart: () -> Unit,
    onMove: (dx: Int, dy: Int) -> Unit,
    onClick: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onBack: () -> Unit = {},
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .recessedWell(scheme.surfaceContainer, scheme.surfaceContainerLowest, scheme.onSurface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(26.dp)),
    ) {
        // Centre — pointer move + tap-to-click.
        Box(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Touchpad — drag to move the pointer, tap to click" }
                .pointerInput(Unit) {
                    var carryX = 0f
                    var carryY = 0f
                    detectDragGestures(
                        onDragStart = {
                            carryX = 0f
                            carryY = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTouchStart()
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            carryX += drag.x * GAIN
                            carryY += drag.y * GAIN
                            val dx = carryX.roundToInt()
                            val dy = carryY.roundToInt()
                            if (dx != 0 || dy != 0) {
                                carryX -= dx
                                carryY -= dy
                                onMove(dx, dy)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            // Centre OK ring — the visible click affordance (a tap anywhere on the pad also clicks).
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
        }

        // Right edge — volume (drawn after the centre so edge touches win).
        EdgeRocker(
            label = "Volume",
            hint = "VOL",
            onUp = onVolumeUp,
            onDown = onVolumeDown,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(EDGE_WIDTH),
        )
        // Back — bottom-left corner tap (replaces channel; visible affordance per 019).
        BackCorner(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomStart).padding(Space.m),
        )

        if (showHint) {
            GestureHintCard(
                onDismiss = onDismissHint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(Space.m),
            )
        }
    }
}

/**
 * The pad's recessed, textured surface (design-system §5.2): a concave radial (lighter centre →
 * darker edge), a faint 7dp dot texture, and an inset top shadow + highlight so it reads as a well.
 * Drawn once via [drawWithCache] (the tile + brushes are cached) so dragging stays cheap.
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
            drawRect(Color.White.copy(alpha = 0.35f), size = Size(size.width, 1.dp.toPx())) // top highlight
        }
    }

/**
 * A transparent edge strip with a faint always-on affordance (▲ HINT ▼). Vertical drag emits stepped
 * up/down; a tap hits the upper/lower half.
 */
@Composable
private fun EdgeRocker(
    label: String,
    hint: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Box(
        modifier
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { acc = 0f },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        acc += dy
                        // Drag up = "up", drag down = "down"; emit a step per STEP_PX of travel.
                        while (acc <= -STEP_PX) {
                            acc += STEP_PX
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUp()
                        }
                        while (acc >= STEP_PX) {
                            acc -= STEP_PX
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDown()
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (offset.y < size.height / 2f) onUp() else onDown()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = faint, modifier = Modifier.height(22.dp))
            Text(hint, style = MaterialTheme.typography.labelMedium, color = faint, modifier = Modifier.rotate(-90f))
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = faint, modifier = Modifier.height(22.dp))
        }
    }
}

/** Bottom-left Back corner — a visible tap target (replaces the old channel edge). */
@Composable
private fun BackCorner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.semantics { contentDescription = "Back" },
    ) {
        Row(
            Modifier.padding(horizontal = Space.m, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.height(18.dp))
            Text("Back", style = MaterialTheme.typography.labelMedium)
        }
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
        Column(
            Modifier.padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text("Good to know", style = MaterialTheme.typography.titleMedium)
            Text(
                "Swipe the right edge for volume; tap the bottom-left corner for Back. Everything else is a tap.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Got it") }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun GesturePadPreview() {
    LGRemoteTheme {
        GesturePad(
            onTouchStart = {},
            onMove = { _, _ -> },
            onClick = {},
            onVolumeUp = {},
            onVolumeDown = {},
            onBack = {},
            showHint = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
