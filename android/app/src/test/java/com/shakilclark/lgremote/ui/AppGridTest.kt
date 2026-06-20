package com.shakilclark.lgremote.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behaviour tests for the dynamic app grid. JVM via Robolectric. Apps are given null icons so the
 * tiles fall back to letter marks (no network); titles + tap routing are asserted regardless of
 * image loading.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AppGridTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun shows_apps_and_launches_the_tapped_one() {
        var launched: TvApp? = null
        val apps = listOf(
            TvApp("netflix", "Netflix", null),
            TvApp("youtube.leanback.v4", "YouTube", null),
        )
        compose.setContent { LGRemoteTheme { AppGrid(apps = apps, onLaunch = { launched = it }) } }
        compose.onNodeWithText("YouTube").assertExists()
        compose.onNodeWithContentDescription("Netflix").assertExists()
        compose.onNodeWithContentDescription("Netflix").performClick()
        assertEquals("netflix", launched?.id)
    }

    @Test
    fun empty_list_shows_a_message() {
        compose.setContent { LGRemoteTheme { AppGrid(apps = emptyList(), onLaunch = {}) } }
        compose.onNodeWithText("No apps found").assertExists()
    }
}
