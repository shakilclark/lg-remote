package com.shakilclark.lgremote.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Behaviour tests for the redesigned remote. JVM via Robolectric — no emulator. Body = pure D-pad +
 * volume; everything else is the icon bottom bar (found by contentDescription). One screen, no
 * scroll. The remote isn't shown when disconnected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class RemoteScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val connected = ConnectionState.Connected(volume = 10, muted = false)

    @Test
    fun connected_controls_are_present() {
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {})
            }
        }
        compose.onNodeWithText("OK").assertExists() // D-pad centre
        compose.onNodeWithContentDescription("Mute").assertExists() // volume strip
        // Transport row.
        listOf("Rewind", "Play or pause", "Fast forward").forEach {
            compose.onNodeWithContentDescription(it).assertExists()
        }
        // Bottom bar.
        listOf("Back", "Home", "Pad", "Apps", "Inputs", "Settings").forEach {
            compose.onNodeWithContentDescription(it).assertExists()
        }
    }

    @Test
    fun play_pause_fires_its_callback() {
        var plays = 0
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {}, onPlayPause = { plays++ })
            }
        }
        compose.onNodeWithContentDescription("Play or pause").performClick()
        assertEquals(1, plays)
    }

    @Test
    fun ok_fires_enter_nav() {
        var nav: NavButton? = null
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {}, onNav = { nav = it })
            }
        }
        compose.onNodeWithText("OK").performClick()
        assertEquals(NavButton.ENTER, nav)
    }

    @Test
    fun bottom_bar_back_and_home_fire_nav() {
        val navs = mutableListOf<NavButton>()
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {}, onNav = { navs += it })
            }
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Home").performClick()
        assertEquals(listOf(NavButton.BACK, NavButton.HOME), navs)
    }

    @Test
    fun mute_fires_its_callback() {
        var mutes = 0
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = { mutes++ })
            }
        }
        compose.onNodeWithContentDescription("Mute").performClick()
        assertEquals(1, mutes)
    }

    @Test
    fun settings_opens_tv_settings() {
        var opened = 0
        compose.setContent {
            LGRemoteTheme {
                RemoteScreen(state = connected, onVolumeUp = {}, onVolumeDown = {}, onToggleMute = {}, onOpenTvSettings = { opened++ })
            }
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        assertEquals(1, opened)
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
        compose.onNodeWithText("OK").assertDoesNotExist()
    }
}
