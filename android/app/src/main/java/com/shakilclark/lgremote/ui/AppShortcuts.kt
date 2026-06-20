package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.tv.AppKey
import com.shakilclark.lgremote.ui.components.ControlKey
import com.shakilclark.lgremote.ui.theme.Space

/** One-tap YouTube / Netflix launch (US6). Neutral chips; brand colour lives in the glyph only. */
@Composable
fun AppShortcuts(onLaunch: (AppKey) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.l)) {
        ShortcutChip("YouTube", Color(0xFFFF0033), Modifier.weight(1f)) { onLaunch(AppKey.YouTube) }
        ShortcutChip("Netflix", Color(0xFFE50914), Modifier.weight(1f)) { onLaunch(AppKey.Netflix) }
    }
}

@Composable
private fun RowScope.ShortcutChip(label: String, brand: Color, modifier: Modifier, onClick: () -> Unit) {
    ControlKey(onClick = onClick, modifier = modifier.height(56.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = brand, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
