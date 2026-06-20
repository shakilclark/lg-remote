package com.shakilclark.lgremote.ui.theme

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Motion is physical, not decorative (design-system §6). Spatial = anything that moves/scales
 * (springs, slight overshoot); effects = color/alpha (tweens, no overshoot). Use these everywhere
 * so the app feels consistently "designed".
 */
object MotionSpecs {
    val spatialFast = spring<Float>(dampingRatio = 0.55f, stiffness = 1400f) // button press
    val spatialDefault = spring<Float>(dampingRatio = 0.70f, stiffness = 700f) // panel / cluster
    val spatialDp = spring<Dp>(dampingRatio = 0.55f, stiffness = 1400f) // corner morph
    val effects = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
    val emphasized = tween<Float>(durationMillis = 450, easing = EaseInOutCubic) // screen transitions
}

/** 4dp-base spacing scale (design-system §5.2). */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}
