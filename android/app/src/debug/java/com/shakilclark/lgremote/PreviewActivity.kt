package com.shakilclark.lgremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.App
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

/**
 * Debug-only live preview of the connected remote with sample state — lets us render the full
 * UI (banner, volume, playback, D-pad, shortcuts, inputs) without a paired TV.
 * Launch: `adb shell am start -n com.shakilclark.lgremote/.PreviewActivity`.
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { LGRemoteTheme { Surface(Modifier.fillMaxSize()) { PreviewRemote() } } }
    }
}

@Composable
private fun PreviewRemote() {
    App(
        ui = UiState(
            connection = ConnectionState.Connected(volume = 13, muted = false),
            activeTvName = "Living Room TV",
            hasActiveTv = true,
        ),
        onConnect = { _, _ -> },
        onRetry = {},
        inputs = listOf(
            TvInput("HDMI_1", "HDMI 1", connected = false),
            TvInput("HDMI_2", "PS4 Game Console", connected = true),
            TvInput("HDMI_3", "Soundbar", connected = false),
        ),
    )
}
