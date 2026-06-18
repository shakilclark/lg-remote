package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.AppKey

/** One-tap YouTube / Netflix launch (US6). */
@Composable
fun AppShortcuts(onLaunch: (AppKey) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ShortcutButton("YouTube", Color(0xFFFF0033), Modifier.weight(1f)) { onLaunch(AppKey.YouTube) }
        ShortcutButton("Netflix", Color(0xFFE50914), Modifier.weight(1f)) { onLaunch(AppKey.Netflix) }
    }
}

@Composable
private fun ShortcutButton(label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(60.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = accent)
        Text("  $label")
    }
}
