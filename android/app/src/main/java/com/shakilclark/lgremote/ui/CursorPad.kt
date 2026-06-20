package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Muted

/**
 * Hold-to-move motion cursor (US5 / T042). Pressing the pad starts the sensor stream and parks
 * it on release (so the pointer never drifts — acceptance #3); the Click button taps the focused
 * item. The pad lights up while held.
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Motion cursor", color = Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .weight(2f)
                    .height(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (held) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
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
                    color = if (held) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(72.dp),
            ) { Text("Click") }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CursorPadPreview() {
    LGRemoteTheme { CursorPad(onStart = {}, onStop = {}, onClick = {}) }
}
