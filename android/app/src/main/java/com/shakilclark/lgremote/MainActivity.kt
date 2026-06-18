package com.shakilclark.lgremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shakilclark.lgremote.ui.App
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LGRemoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val ui by viewModel.uiState.collectAsStateWithLifecycle()
                    App(
                        ui = ui,
                        onConnect = viewModel::connectTo,
                        onRetry = viewModel::retry,
                    )
                }
            }
        }
    }
}
