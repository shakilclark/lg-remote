package com.shakilclark.lgremote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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

/**
 * A symmetrical, full-width +-shaped directional pad (redesign). A 3×3 grid of equal square cells:
 * Up/Down/Left/Right are identical squares around the accented OK centre, with the four corners
 * empty. All non-directional actions live in the bottom bar, so there are no diagonal neighbours to
 * mis-tap. OK sends ENTER, which also serves play/pause in webOS media.
 */
@Composable
fun DirectionPad(onNav: (NavButton) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            EmptyCell()
            GridKey(Icons.Filled.KeyboardArrowUp, "Up") { onNav(NavButton.UP) }
            EmptyCell()
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GridKey(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left") { onNav(NavButton.LEFT) }
            OkCell { onNav(NavButton.ENTER) }
            GridKey(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right") { onNav(NavButton.RIGHT) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            EmptyCell()
            GridKey(Icons.Filled.KeyboardArrowDown, "Down") { onNav(NavButton.DOWN) }
            EmptyCell()
        }
    }
}

/** One empty 1/3-width corner cell — keeps the grid square. */
@Composable
private fun RowScope.EmptyCell() = Spacer(Modifier.weight(1f).aspectRatio(1f))

/** One arrow cell — a square that's exactly 1/3 of the row width. */
@Composable
private fun RowScope.GridKey(icon: ImageVector, label: String, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = Modifier.weight(1f).aspectRatio(1f)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
    }
}

/** The persistently-accented anchor (§7.2): a primaryContainer circle filling the centre cell. */
@Composable
private fun RowScope.OkCell(onClick: () -> Unit) {
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
            .weight(1f)
            .aspectRatio(1f)
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
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DirectionPadPreview() {
    LGRemoteTheme { DirectionPad(onNav = {}) }
}
