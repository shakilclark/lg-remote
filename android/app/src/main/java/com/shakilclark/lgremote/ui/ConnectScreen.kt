package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme
import com.shakilclark.lgremote.ui.theme.Muted

/** Manual-IP connect + on-screen pairing status (US1, T017). Discovery is a later slice. */
@Composable
fun ConnectScreen(
    state: ConnectionState,
    onConnect: (address: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ip by remember { mutableStateOf("") }
    val connecting = state is ConnectionState.Connecting
    val pairing = state is ConnectionState.NeedsPairing
    val validIp = ip.isNotBlank()

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📺", fontSize = 46.sp)
        Text("Connect to your TV", fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            if (pairing) (state as ConnectionState.NeedsPairing).message
            else "Enter your LG TV's IP address. It must be on this Wi-Fi.",
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it.trim() },
            label = { Text("TV IP address") },
            placeholder = { Text("192.168.0.9") },
            singleLine = true,
            enabled = !connecting && !pairing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onConnect(ip, ip) },
            enabled = validIp && !connecting && !pairing,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
        ) {
            when {
                pairing -> Text("Accept the prompt on your TV…")
                connecting -> CircularProgressIndicator(modifier = Modifier.height(22.dp))
                else -> Text("Connect")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ConnectPreview() {
    LGRemoteTheme { ConnectScreen(ConnectionState.Disconnected(), { _, _ -> }) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PairingPreview() {
    LGRemoteTheme { ConnectScreen(ConnectionState.NeedsPairing(), { _, _ -> }) }
}
