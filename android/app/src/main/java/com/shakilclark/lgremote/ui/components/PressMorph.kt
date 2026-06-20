package com.shakilclark.lgremote.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.theme.MotionSpecs

/**
 * The tactile signature (design-system §4.2): on press a control morphs rounder and scales down on
 * a spring, then settles. Drive it from a control's [InteractionSource] and pair with a haptic +
 * `indication = null` so the morph itself is the feedback (no ripple).
 */
@Composable
fun Modifier.pressMorph(
    interaction: InteractionSource,
    restCorner: Dp = 20.dp,
    pressedCorner: Dp = 28.dp,
    pressedScale: Float = 0.94f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) pressedCorner else restCorner,
        animationSpec = MotionSpecs.spatialDp,
        label = "pressMorphCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MotionSpecs.spatialFast,
        label = "pressMorphScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(RoundedCornerShape(corner))
}
