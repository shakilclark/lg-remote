package com.shakilclark.lgremote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.ui.components.ControlKey
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.MotionSpecs
import com.shakilclark.lgremote.ui.theme.Space

/** D-pad cluster + OK and a Back/Home/Exit row (US3, design-system §7.2). */
@Composable
fun DPad(onNav: (NavButton) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.l)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .size(260.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
                    .padding(Space.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Arrow(Icons.Filled.KeyboardArrowUp, "Up") { onNav(NavButton.UP) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    Arrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left") { onNav(NavButton.LEFT) }
                    OkKey { onNav(NavButton.ENTER) }
                    Arrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right") { onNav(NavButton.RIGHT) }
                }
                Arrow(Icons.Filled.KeyboardArrowDown, "Down") { onNav(NavButton.DOWN) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            WideNav(Icons.AutoMirrored.Filled.ArrowBack, "Back") { onNav(NavButton.BACK) }
            WideNav(Icons.Filled.Home, "Home") { onNav(NavButton.HOME) }
            WideNav(Icons.Filled.Close, "Exit") { onNav(NavButton.EXIT) }
        }
    }
}

@Composable
private fun Arrow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = Modifier.size(64.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun RowScope.WideNav(icon: ImageVector, label: String, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = Modifier.weight(1f).height(56.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
    }
}

/** The only persistently-accented control (§7.2): 80dp circle, primaryContainer, stronger press. */
@Composable
private fun OkKey(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = MotionSpecs.spatialFast,
        label = "okScale",
    )
    Box(
        Modifier
            .size(80.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "OK",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DPadPreview() {
    LGRemoteTheme { DPad(onNav = {}) }
}
