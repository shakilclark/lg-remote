package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.shakilclark.lgremote.tv.DiscoveredTv
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

/** Auto-detect (SSDP) + manual-IP connect + on-screen pairing status (US1, T017). */
@Composable
fun ConnectScreen(
    state: ConnectionState,
    onConnect: (address: String, name: String) -> Unit,
    discovered: List<DiscoveredTv> = emptyList(),
    scanning: Boolean = false,
    onScan: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var ip by remember { mutableStateOf("") }
    val connecting = state is ConnectionState.Connecting
    val pairing = state is ConnectionState.NeedsPairing
    val busy = connecting || pairing

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📺", fontSize = 46.sp)
        Text("Connect to your TV", fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            if (pairing) (state as ConnectionState.NeedsPairing).message
            else "Scan for your LG TV, or enter its IP. It must be on this Wi-Fi.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )

        // Discovered TVs (tap to connect).
        discovered.forEach { tv ->
            OutlinedButton(
                onClick = { onConnect(tv.address, tv.name) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 10.dp),
            ) {
                Icon(Icons.Rounded.Tv, contentDescription = null)
                Text("  ${tv.name} · ${tv.address}")
            }
        }

        // Scan.
        OutlinedButton(
            onClick = onScan,
            enabled = !scanning && !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (scanning) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("Scanning…")
                }
            } else {
                Text(if (discovered.isEmpty()) "Scan for TVs" else "Scan again")
            }
        }

        // Manual IP fallback.
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it.trim() },
            label = { Text("…or enter IP address") },
            placeholder = { Text("192.168.0.9") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
        Button(
            onClick = { onConnect(ip, ip) },
            enabled = ip.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 12.dp),
        ) {
            when {
                pairing -> Text("Accept the prompt on your TV…")
                connecting -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text("Connecting…")
                }
                else -> Text("Connect")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ConnectPreview() {
    LGRemoteTheme {
        ConnectScreen(
            ConnectionState.Disconnected(),
            { _, _ -> },
            discovered = listOf(DiscoveredTv("192.168.0.9", "LG webOS TV")),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PairingPreview() {
    LGRemoteTheme { ConnectScreen(ConnectionState.NeedsPairing(), { _, _ -> }) }
}
