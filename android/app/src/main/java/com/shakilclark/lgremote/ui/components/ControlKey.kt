package com.shakilclark.lgremote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.MotionSpecs

/**
 * The workhorse remote button (design-system §7.1): neutral `surfaceContainerHigh` fill + `onSurface`
 * glyph by default; on press it morphs (§4.2), shifts to `surfaceContainerHighest` with a faint
 * primary overlay, and the glyph tints to `primary`. Haptic on press-down; ripple off — the morph is
 * the feedback. Size/shape come from [modifier], so it works as a square key or a wide bar.
 */
@Composable
fun ControlKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    pressedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    pressedContentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scheme = MaterialTheme.colorScheme

    val fill = if (pressed) pressedContainerColor else containerColor
    val overlay by animateFloatAsState(
        targetValue = if (pressed) 0.06f else 0f,
        animationSpec = MotionSpecs.spatialFast,
        label = "controlKeyOverlay",
    )
    val glyph by animateColorAsState(
        targetValue = if (pressed) pressedContentColor else contentColor,
        animationSpec = MotionSpecs.effects,
        label = "controlKeyGlyph",
    )

    Box(
        modifier
            .pressMorph(interaction)
            .background(fill)
            .background(scheme.primary.copy(alpha = overlay))
            .clickable(interactionSource = interaction, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides glyph, content = content)
    }
}

@Preview
@Composable
private fun ControlKeyPreview() {
    LGRemoteTheme {
        ControlKey(onClick = {}, modifier = Modifier.size(64.dp)) { Text("OK") }
    }
}
