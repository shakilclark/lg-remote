package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.Muted

/** List + switch external inputs (US7). Loaded on demand via [onLoad]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputSwitcher(
    inputs: List<TvInput>,
    onLoad: () -> Unit,
    onSelect: (TvInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "INPUTS",
            color = Muted,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (inputs.isEmpty()) {
            OutlinedButton(
                onClick = onLoad,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Show inputs") }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                inputs.forEach { input ->
                    FilterChip(
                        selected = input.connected,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(input)
                        },
                        label = { Text(input.label) },
                    )
                }
            }
        }
    }
}
