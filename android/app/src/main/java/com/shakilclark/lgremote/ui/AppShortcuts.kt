package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.tv.AppKey
import com.shakilclark.lgremote.ui.theme.Space

/**
 * Bottom row of app launchers as brand-coloured icon tiles (US6, redesign). Each tile is the app's
 * wordmark initial on its brand colour — recognisable as an app icon, no captions. Left-aligned with
 * room to add more launchers later.
 */
@Composable
fun AppShortcuts(onLaunch: (AppKey) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        AppTile("YouTube", "Y", Color(0xFFFF0000)) { onLaunch(AppKey.YouTube) }
        AppTile("Netflix", "N", Color(0xFFE50914)) { onLaunch(AppKey.Netflix) }
    }
}

@Composable
private fun AppTile(name: String, mark: String, brand: Color, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(brand)
            .semantics { contentDescription = name }
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(mark, color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
    }
}
