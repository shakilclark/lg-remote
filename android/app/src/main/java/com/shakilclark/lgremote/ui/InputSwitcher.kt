package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.TvInput

/**
 * Inputs sheet body (US7, redesign): the list is pre-filtered to inputs that have a connection, so
 * dead/empty HDMI ports never show. Tap a chip to switch source. Loading happens when the sheet
 * opens; this is pure presentation of the given [inputs].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputSwitcher(
    inputs: List<TvInput>,
    onSelect: (TvInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "INPUTS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (inputs.isEmpty()) {
            Text("No connected inputs", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                inputs.forEach { input ->
                    AssistChip(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(input)
                        },
                        label = { Text(input.label) },
                        leadingIcon = { Icon(Icons.Rounded.Tv, contentDescription = null) },
                    )
                }
            }
        }
    }
}
