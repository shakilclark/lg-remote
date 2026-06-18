package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

/** Directional pad + OK (3x3 cross) and a Back/Home/Exit row (US3). */
@Composable
fun DPad(onNav: (NavButton) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.fillMaxWidth().aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CellSpacer(); ArrowCell(Icons.Filled.KeyboardArrowUp, "Up") { onNav(NavButton.UP) }; CellSpacer()
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArrowCell(Icons.Filled.KeyboardArrowLeft, "Left") { onNav(NavButton.LEFT) }
                OkCell { onNav(NavButton.ENTER) }
                ArrowCell(Icons.Filled.KeyboardArrowRight, "Right") { onNav(NavButton.RIGHT) }
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CellSpacer(); ArrowCell(Icons.Filled.KeyboardArrowDown, "Down") { onNav(NavButton.DOWN) }; CellSpacer()
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NavKey(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.weight(1f)) { onNav(NavButton.BACK) }
            NavKey(Icons.Filled.Home, "Home", Modifier.weight(1f)) { onNav(NavButton.HOME) }
            NavKey(Icons.Filled.Close, "Exit", Modifier.weight(1f)) { onNav(NavButton.EXIT) }
        }
    }
}

@Composable
private fun RowScope.CellSpacer() = Spacer(Modifier.weight(1f))

@Composable
private fun RowScope.ArrowCell(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.weight(1f).aspectRatio(1f),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp)) }
}

@Composable
private fun RowScope.OkCell(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = CircleShape,
        modifier = Modifier.weight(1f).aspectRatio(1f),
    ) { Text("OK") }
}

@Composable
private fun NavKey(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(64.dp),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DPadPreview() {
    LGRemoteTheme { DPad(onNav = {}) }
}
