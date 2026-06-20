package com.shakilclark.lgremote.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.TvInput
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual-regression goldens for the M3 Expressive UI (T3). JVM-rendered via Robolectric — no
 * emulator. Record: `./gradlew :app:recordRoborazziDebug`; verify (CI): `verifyRoborazziDebug`.
 * Goldens live in `src/test/screenshots/` and are committed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { LGRemoteTheme { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun remote_connected() = shot("remote_connected") {
        RemoteScreen(
            state = ConnectionState.Connected(volume = 13, muted = false),
            onVolumeUp = {},
            onVolumeDown = {},
            onToggleMute = {},
            onPlayPause = {},
            inputs = listOf(
                TvInput("HDMI_1", "HDMI 1", connected = false),
                TvInput("HDMI_2", "PS4 Game Console", connected = true),
            ),
        )
    }

    @Test
    fun dpad_cluster() = shot("dpad_cluster") { DPad(onNav = {}) }

    @Test
    fun cursor_pad() = shot("cursor_pad") { CursorPad(onStart = {}, onStop = {}, onClick = {}) }

    @Test
    fun banner_connected() = shot("banner_connected") {
        ConnectionBanner(
            state = ConnectionState.Connected(volume = 13, muted = false),
            tvName = "Living Room TV",
            hasActiveTv = true,
        )
    }
}
