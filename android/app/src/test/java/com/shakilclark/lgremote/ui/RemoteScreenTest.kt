package com.shakilclark.lgremote.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.shakilclark.lgremote.UiState
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.tv.NavButton
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behaviour tests for the connected remote and routing (T2 / closes T045). Runs on the JVM via
 * Robolectric — no emulator. Asserts controls exist, fire their callbacks, and that the remote
 * is not shown when the TV isn't connected. RemoteScreen scrolls, so we assert existence and
 * scroll controls into view before clicking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class RemoteScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val connected = ConnectionState.Connected(volume = 10, muted = false)

    @Test
    fun connected_controls_are_present() {
        compose.setContent { LGRemoteTheme { RemoteScreen(state = connected, {}, {}, {}, {}) } }
        compose.onNodeWithText("OK").assertExists()
        compose.onNodeWithText("Mute").assertExists()
        compose.onNodeWithText("Click").assertExists()
        compose.onNodeWithText("Hold to move").assertExists()
    }

    @Test
    fun ok_fires_enter_nav() {
        var nav: NavButton? = null
        compose.setContent {
            LGRemoteTheme { RemoteScreen(state = connected, {}, {}, {}, {}, onNav = { nav = it }) }
        }
        compose.onNodeWithText("OK").performScrollTo().performClick()
        assertEquals(NavButton.ENTER, nav)
    }

    @Test
    fun mute_and_cursor_click_fire_their_callbacks() {
        var mutes = 0
        var clicks = 0
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(
                    state = connected,
                    onVolumeUp = {},
                    onVolumeDown = {},
                    onToggleMute = { mutes++ },
                    onPlayPause = {},
                    onCursorClick = { clicks++ },
                )
            }
        }
        compose.onNodeWithText("Mute").performScrollTo().performClick()
        compose.onNodeWithText("Click").performScrollTo().performClick()
        assertEquals(1, mutes)
        assertEquals(1, clicks)
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
        compose.onNodeWithText("OK").assertDoesNotExist()
    }
}
