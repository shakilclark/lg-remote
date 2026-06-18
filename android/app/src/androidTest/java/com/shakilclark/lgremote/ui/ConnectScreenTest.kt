package com.shakilclark.lgremote.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shakilclark.lgremote.connection.ConnectionState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Compose UI test (T020). Runs on a device/emulator: `./gradlew :app:connectedDebugAndroidTest`.
 */
class ConnectScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun connectButtonEnablesOnceIpEntered_andEmitsAddress() {
        var connectedTo: String? = null
        rule.setContent {
            ConnectScreen(
                state = ConnectionState.Disconnected(),
                onConnect = { address, _ -> connectedTo = address },
            )
        }

        rule.onNodeWithText("Connect").assertIsNotEnabled()
        rule.onNodeWithText("TV IP address").performTextInput("192.168.0.9")
        rule.onNodeWithText("Connect").assertIsEnabled()
        rule.onNodeWithText("Connect").performClick()
        assertEquals("192.168.0.9", connectedTo)
    }

    @Test
    fun pairingStateShowsAcceptOnTvCopy() {
        rule.setContent {
            ConnectScreen(state = ConnectionState.NeedsPairing(), onConnect = { _, _ -> })
        }
        // The pairing copy appears in both the description and the button label.
        val matches = rule.onAllNodesWithText("Accept the prompt on your TV", substring = true)
            .fetchSemanticsNodes()
        assert(matches.isNotEmpty())
    }
}
