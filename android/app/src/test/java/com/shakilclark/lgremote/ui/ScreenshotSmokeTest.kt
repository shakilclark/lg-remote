package com.shakilclark.lgremote.ui

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke test for the Roborazzi + Robolectric screenshot pipeline (T1). Real component goldens
 * land in T3, after the design system. Record: `./gradlew :app:recordRoborazziDebug`;
 * verify on CI: `./gradlew :app:verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theme_smoke() {
        compose.setContent {
            LGRemoteTheme {
                Surface { Text("LG Remote") }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/theme_smoke.png")
    }
}
