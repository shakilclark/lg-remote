package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.ui.components.ControlKey
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Space

/**
 * Hold-to-move motion cursor (US5 / §8). Pressing the pad starts the sensor stream and parks it on
 * release (so the pointer never drifts); the Click ControlKey taps the focused item. The pad lights
 * to the accent while held.
 */
@Composable
fun CursorPad(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var held by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Text(
            "Motion cursor",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            Box(
                Modifier
                    .weight(2f)
                    .height(72.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (held) scheme.primary else scheme.surfaceContainerHigh)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                held = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStart()
                                tryAwaitRelease()
                                held = false
                                onStop()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (held) "Move the phone…" else "Hold to move",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (held) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
            ControlKey(onClick = onClick, modifier = Modifier.weight(1f).height(72.dp)) {
                Text("Click", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CursorPadPreview() {
    LGRemoteTheme { CursorPad(onStart = {}, onStop = {}, onClick = {}) }
}
