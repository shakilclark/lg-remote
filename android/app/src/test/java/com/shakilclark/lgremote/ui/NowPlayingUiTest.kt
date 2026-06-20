package com.shakilclark.lgremote.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shakilclark.lgremote.tv.NowPlaying
import com.shakilclark.lgremote.tv.PlayState
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Behaviour tests for the now-playing strip + sheet (004). JVM via Robolectric; null icon → letter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class NowPlayingUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val np = NowPlaying("netflix", "Netflix", null, PlayState.Paused)

    @Test
    fun strip_shows_app_and_expands_on_tap() {
        var expanded = 0
        compose.setContent { LGRemoteTheme { NowPlayingStrip(nowPlaying = np, onExpand = { expanded++ }) } }
        compose.onNodeWithText("Netflix").assertExists()
        compose.onNodeWithText("Paused").assertExists()
        compose.onNodeWithContentDescription("Now playing: Netflix").performClick()
        assertEquals(1, expanded)
    }

    @Test
    fun sheet_transport_fires_callbacks() {
        var plays = 0
        var stops = 0
        compose.setContent {
            LGRemoteTheme {
                NowPlayingSheetContent(
                    nowPlaying = np,
                    onRewind = {},
                    onPlayPause = { plays++ },
                    onFastForward = {},
                    onStop = { stops++ },
                )
            }
        }
        compose.onNodeWithContentDescription("Play or pause").performClick()
        compose.onNodeWithContentDescription("Stop").performClick()
        assertEquals(1, plays)
        assertEquals(1, stops)
    }
}
