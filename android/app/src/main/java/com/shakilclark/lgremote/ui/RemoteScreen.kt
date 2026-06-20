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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.Muted
import com.shakilclark.lgremote.tv.AppKey
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

/** The connected remote (US2 controls + US3 nav + US6 shortcuts + US7 inputs; US5 adds cursor). */
@Composable
fun RemoteScreen(
    state: ConnectionState.Connected,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onPlayPause: () -> Unit,
    onNav: (NavButton) -> Unit = {},
    onLaunchApp: (AppKey) -> Unit = {},
    inputs: List<TvInput> = emptyList(),
    onLoadInputs: () -> Unit = {},
    onSelectInput: (TvInput) -> Unit = {},
    onCursorStart: () -> Unit = {},
    onCursorStop: () -> Unit = {},
    onCursorClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VolumePad(
                onUp = onVolumeUp,
                onDown = onVolumeDown,
                modifier = Modifier.weight(1f),
            )
            PlaybackBar(
                muted = state.muted,
                onToggleMute = onToggleMute,
                onPlayPause = onPlayPause,
                modifier = Modifier.weight(1f),
            )
        }
        state.volume?.let { vol ->
            Text(
                "Volume $vol",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DPad(onNav = onNav)
        CursorPad(onStart = onCursorStart, onStop = onCursorStop, onClick = onCursorClick)
        AppShortcuts(onLaunch = onLaunchApp)
        InputSwitcher(inputs = inputs, onLoad = onLoadInputs, onSelect = onSelectInput)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun RemotePreview() {
    LGRemoteTheme {
        RemoteScreen(ConnectionState.Connected(volume = 13, muted = false), {}, {}, {}, {})
    }
}
