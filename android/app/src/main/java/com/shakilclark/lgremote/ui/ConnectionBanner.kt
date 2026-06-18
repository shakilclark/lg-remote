package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.AccentSoft
import com.shakilclark.lgremote.ui.theme.Edge
import com.shakilclark.lgremote.ui.theme.Muted
import com.shakilclark.lgremote.ui.theme.Ok
import com.shakilclark.lgremote.ui.theme.Panel
import com.shakilclark.lgremote.ui.theme.Warn

/** Always-visible connection-state chip (FR-009). Title + sub-line, with a status dot. */
@Composable
fun ConnectionBanner(state: ConnectionState, tvName: String?, modifier: Modifier = Modifier) {
    val (dot, title, sub) = bannerContent(state, tvName)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Edge, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.size(11.dp).clip(CircleShape).background(dot))
        androidx.compose.foundation.layout.Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (sub != null) Text(sub, color = Muted, fontSize = 12.5.sp)
        }
    }
}

private data class Banner(val dot: Color, val title: String, val sub: String?)

private fun bannerContent(state: ConnectionState, tvName: String?): Banner = when (state) {
    is ConnectionState.Connected -> Banner(Ok, tvName ?: "Connected", "Connected")
    ConnectionState.Connecting -> Banner(Warn, tvName ?: "Connecting…", "Connecting…")
    is ConnectionState.NeedsPairing -> Banner(Warn, tvName ?: "Pairing", state.message)
    is ConnectionState.Disconnected -> Banner(AccentSoft, tvName ?: "Disconnected", state.message)
    is ConnectionState.OffNetwork -> Banner(AccentSoft, "Off network", state.message)
    ConnectionState.PermissionRequired -> Banner(AccentSoft, "Permission needed", "Allow local-network access")
}
