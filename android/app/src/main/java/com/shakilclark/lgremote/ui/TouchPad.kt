package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space
import kotlin.math.roundToInt

/** Finger travel → pointer travel gain. ~1.6 feels close to a laptop trackpad on the TV. */
private const val GAIN = 1.6f

/**
 * Large trackpad surface that drives the LG on-screen pointer (US5, redesign — replaces the gyro
 * motion cursor). Drag anywhere to move the pointer (deltas sent on the same pointer socket the
 * D-pad uses), tap to click. [onTouchStart] opens the socket as a drag begins so the first moves
 * land. Sub-pixel drag is accumulated so slow movement stays smooth.
 */
@Composable
fun TouchPad(
    onTouchStart: () -> Unit,
    onMove: (dx: Int, dy: Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.large)
            .semantics { contentDescription = "Touchpad" }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Icon(
                Icons.Filled.TouchApp,
                contentDescription = null,
                modifier = Modifier.height(28.dp),
                tint = scheme.onSurfaceVariant,
            )
            Text(
                "Drag to move · tap to click",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 300)
@Composable
private fun TouchPadPreview() {
    LGRemoteTheme {
        TouchPad(onTouchStart = {}, onMove = { _, _ -> }, onClick = {}, modifier = Modifier.fillMaxSize())
    }
}
