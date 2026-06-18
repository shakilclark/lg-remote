package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

/** The connected remote (US2 controls; US3/US5/US6/US7 add to this). */
@Composable
fun RemoteScreen(
    state: ConnectionState.Connected,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VolumePad(
                volume = state.volume,
                onUp = onVolumeUp,
                onDown = onVolumeDown,
                modifier = Modifier.weight(1.15f),
            )
            PlaybackBar(
                muted = state.muted,
                onToggleMute = onToggleMute,
                onPlayPause = onPlayPause,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun RemotePreview() {
    LGRemoteTheme {
        RemoteScreen(ConnectionState.Connected(volume = 13, muted = false), {}, {}, {}, {})
    }
}
