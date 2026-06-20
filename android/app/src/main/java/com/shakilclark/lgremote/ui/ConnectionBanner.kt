package com.shakilclark.lgremote.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.AccentSoft
import com.shakilclark.lgremote.ui.theme.Muted
import com.shakilclark.lgremote.ui.theme.Ok
import com.shakilclark.lgremote.ui.theme.Space
import com.shakilclark.lgremote.ui.theme.Warn

/** Always-visible connection-state chip (FR-009, design-system §7.6). Title + sub-line + status dot;
 *  the dot breathes while connected. */
@Composable
fun ConnectionBanner(
    state: ConnectionState,
    tvName: String?,
    hasActiveTv: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val banner = bannerContent(state, tvName, hasActiveTv)
    val pulse = rememberInfiniteTransition(label = "dotPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "dotAlpha",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Spacer(
            Modifier
                .size(11.dp)
                .alpha(if (banner.pulse) dotAlpha else 1f)
                .clip(CircleShape)
                .background(banner.dot),
        )
        Column {
            Text(banner.title, style = MaterialTheme.typography.titleMedium)
            banner.sub?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class Banner(val dot: Color, val title: String, val sub: String?, val pulse: Boolean)

private fun bannerContent(state: ConnectionState, tvName: String?, hasActiveTv: Boolean): Banner = when (state) {
    is ConnectionState.Connected -> Banner(Ok, tvName ?: "Connected", "Connected", pulse = true)
    ConnectionState.Connecting -> Banner(Warn, tvName ?: "Connecting…", "Connecting…", pulse = false)
    is ConnectionState.NeedsPairing -> Banner(Warn, tvName ?: "Pairing", state.message, pulse = false)
    // First run (no remembered TV) shouldn't look like an error — keep it neutral.
    is ConnectionState.Disconnected ->
        if (hasActiveTv) {
            Banner(AccentSoft, tvName ?: "Disconnected", state.message, pulse = false)
        } else {
            Banner(Muted, "Not connected", "Add your TV to get started", pulse = false)
        }
    is ConnectionState.OffNetwork -> Banner(AccentSoft, "Off network", state.message, pulse = false)
    ConnectionState.PermissionRequired -> Banner(AccentSoft, "Permission needed", "Allow local-network access", pulse = false)
}
