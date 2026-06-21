package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.NowPlaying
import com.shakilclark.lgremote.tv.PlayState
import com.shakilclark.lgremote.tv.TvApp
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
 *
 * A small [changeThreshold] absorbs cross-platform font anti-aliasing (goldens may be recorded on
 * macOS but verified on Linux CI) while still catching real layout/colour regressions. Components
 * with looping animations (e.g. the connection-pulse banner) are intentionally not screenshotted.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.05f),
    )

    private fun shot(name: String, content: @Composable () -> Unit) {
        // Wrap in a surface-coloured Surface so goldens render on the real fallback background
        // (dynamic-off light = #FDF7FF), matching the on-device look and the fidelity refs — not the
        // bare Robolectric #FAFAFA default. tonalElevation 0 keeps it flat (no elevation tint).
        compose.setContent {
            LGRemoteTheme {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = options)
    }

    @Test
    fun remote_connected() = shot("remote_connected") {
        RemoteScreen(
            state = ConnectionState.Connected(volume = 13, muted = false),
            onVolumeUp = {},
            onVolumeDown = {},
            onToggleMute = {},
            inputs = listOf(
                TvInput("HDMI_1", "HDMI 1", connected = false),
                TvInput("HDMI_2", "PS4 Game Console", connected = true),
            ),
        )
    }

    @Test
    fun command_sheet() = shot("command_sheet") {
        CommandSheet(
            onNav = {},
            apps = emptyList(),
            onLaunchApp = {},
            inputs = emptyList(),
            onSelectInput = {},
            onOpenTvSettings = {},
        )
    }

    @Test
    fun app_grid() = shot("app_grid") {
        // Null icons → deterministic letter-tile fallback (no network in the golden).
        AppGrid(
            apps = listOf(
                TvApp("youtube.leanback.v4", "YouTube", null),
                TvApp("netflix", "Netflix", null),
                TvApp("disney", "Disney+", null),
                TvApp("iplayer", "BBC iPlayer", null),
                TvApp("prime", "Prime Video", null),
            ),
            onLaunch = {},
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )
    }

    @Test
    fun now_playing_bar() = shot("now_playing_bar") {
        NowPlayingBar(
            nowPlaying = NowPlaying("netflix", "Netflix", null, PlayState.Paused),
            onExpand = {},
            onPlayPause = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Test
    fun now_playing_cover() = shot("now_playing_cover") {
        NowPlayingSheetContent(
            nowPlaying = NowPlaying("youtube", "YouTube", null, PlayState.Playing),
            onRewind = {},
            onPlayPause = {},
            onFastForward = {},
            onStop = {},
        )
    }
}
