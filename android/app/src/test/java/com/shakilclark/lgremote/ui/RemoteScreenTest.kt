package com.shakilclark.lgremote.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shakilclark.lgremote.UiState
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behaviour tests for the redesigned remote (010 Direction C). JVM via Robolectric — no emulator.
 * Home = the gesture pad + a "More controls" grip; the D-pad, apps, inputs, mute and Settings live
 * in the pull-up command sheet (tested directly as leaf composables to avoid driving a
 * ModalBottomSheet). Back is intentionally not on the home yet (affordance being redesigned). The
 * remote isn't shown when disconnected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class RemoteScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val connected = ConnectionState.Connected(volume = 10, muted = false)

    @Test
    fun home_shows_gesture_pad_and_grip() {
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {})
            }
        }
        compose.onNodeWithContentDescription("Touchpad", substring = true).assertExists() // gesture pad
        compose.onNodeWithContentDescription("More controls").assertExists() // grip → command sheet
    }

    @Test
    fun command_sheet_shows_segments() {
        compose.setContent {
            LGRemoteTheme {
                CommandSheet(apps = emptyList(), onLaunchApp = {}, inputs = emptyList(), onSelectInput = {})
            }
        }
        // Back/Home/Mute/Settings moved to the clickpad corners (022); the sheet is the segmented tabs.
        compose.onNodeWithText("Apps").assertExists()
        compose.onNodeWithText("Inputs").assertExists()
    }

    @Test
    fun reconnecting_shows_the_chip() {
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {}, reconnecting = true)
            }
        }
        compose.onNodeWithContentDescription("Reconnecting").assertExists()
    }

    @Test
    fun connected_has_no_reconnecting_chip() {
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {})
            }
        }
        compose.onNodeWithContentDescription("Reconnecting").assertDoesNotExist()
    }

    @Test
    fun not_connected_shows_reconnect_not_the_remote() {
        compose.setContent {
            LGRemoteTheme {
                App(
                    ui = UiState(
                        connection = ConnectionState.Disconnected(),
                        activeTvName = "Living Room TV",
                        hasActiveTv = true,
                    ),
                    onConnect = { _, _ -> },
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithText("Try now").assertIsDisplayed() // ReconnectView, not the remote
        compose.onNodeWithContentDescription("More controls").assertDoesNotExist() // remote grip absent
    }
}
