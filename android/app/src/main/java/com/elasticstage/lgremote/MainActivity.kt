package com.elasticstage.lgremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elasticstage.lgremote.ui.theme.LGRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LGRemoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Placeholder shell — RemoteScreen / ConnectScreen wire in from US1 onward.
                    PlaceholderScreen()
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("LG Remote — setup complete")
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PlaceholderPreview() {
    LGRemoteTheme { PlaceholderScreen() }
}
