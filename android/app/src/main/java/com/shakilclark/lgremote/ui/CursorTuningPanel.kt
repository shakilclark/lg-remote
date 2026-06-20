package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shakilclark.lgremote.cursor.CursorTuning
import com.shakilclark.lgremote.ui.theme.Space
import kotlin.math.roundToInt

/**
 * Debug-only live tuning for the motion cursor (US5 on-device tuning). Adjusts [CursorTuning]
 * in real time so sensitivity, dead-zone and axis direction can be dialled in against the real TV
 * without rebuilding. Wired into RemoteScreen behind `BuildConfig.DEBUG`. Once values feel right,
 * note them and bake them into CursorMath/CursorTuning defaults.
 */
@Composable
fun CursorTuningPanel(modifier: Modifier = Modifier) {
    var sensitivity by remember { mutableFloatStateOf(CursorTuning.sensitivity.toFloat()) }
    var deadZone by remember { mutableFloatStateOf(CursorTuning.deadZone.toFloat()) }
    var invertX by remember { mutableStateOf(CursorTuning.invertX) }
    var invertY by remember { mutableStateOf(CursorTuning.invertY) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            "Cursor tuning (debug)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Sensitivity: ${sensitivity.roundToInt()} px/rad", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = sensitivity,
            onValueChange = { sensitivity = it; CursorTuning.sensitivity = it.toDouble() },
            valueRange = 300f..3000f,
        )
        Text("Dead-zone: ${"%.4f".format(deadZone)} rad", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = deadZone,
            onValueChange = { deadZone = it; CursorTuning.deadZone = it.toDouble() },
            valueRange = 0f..0.02f,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Text("Invert X", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = invertX, onCheckedChange = { invertX = it; CursorTuning.invertX = it })
            Text("Invert Y", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = invertY, onCheckedChange = { invertY = it; CursorTuning.invertY = it })
        }
    }
}
