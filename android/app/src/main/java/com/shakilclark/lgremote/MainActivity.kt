package com.shakilclark.lgremote

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.shakilclark.lgremote.ui.components.LocalHapticsEnabled
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shakilclark.lgremote.connection.ConnectionState
import com.shakilclark.lgremote.ui.App
import com.shakilclark.lgremote.ui.ScreenWakeEffect
import com.shakilclark.lgremote.ui.rememberInteractionSignal
import com.shakilclark.lgremote.ui.trackInteractions
import com.shakilclark.lgremote.ui.theme.LGRemoteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            LGRemoteTheme(theme = appTheme, mode = themeMode) {
                val snackbar = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    viewModel.messages.collect { snackbar.showSnackbar(it) }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { padding ->
                    val ui by viewModel.uiState.collectAsStateWithLifecycle()
                    val inputs by viewModel.inputs.collectAsStateWithLifecycle()
                    val apps by viewModel.apps.collectAsStateWithLifecycle()
                    val appsLoading by viewModel.appsLoading.collectAsStateWithLifecycle()
                    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
                    val discovered by viewModel.discovered.collectAsStateWithLifecycle()
                    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
                    val showGestureHint by viewModel.showGestureHint.collectAsStateWithLifecycle()
                    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
                    val alwaysOn by viewModel.alwaysOn.collectAsStateWithLifecycle()
                    val tvAddress by viewModel.activeAddress.collectAsStateWithLifecycle()

                    // Always-on (Settings → Behaviour): keep the screen awake + auto-dim while connected,
                    // so the remote stays reachable without re-waking/unlocking the phone.
                    val interactions = rememberInteractionSignal()
                    ScreenWakeEffect(
                        enabled = alwaysOn && ui.connection is ConnectionState.Connected,
                        interactions = interactions,
                    )

                    // Draw over the keyguard (and wake the screen) when always-on is on, so a tap from
                    // the launcher / QS tile lands straight in the remote with no unlock step.
                    LaunchedEffect(alwaysOn) {
                        this@MainActivity.setShowWhenLocked(alwaysOn)
                        this@MainActivity.setTurnScreenOn(alwaysOn)
                    }

                    CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
                    App(
                        ui = ui,
                        onConnect = viewModel::connectTo,
                        onRetry = { viewModel.retry() },
                        discovered = discovered,
                        scanning = scanning,
                        onScan = viewModel::discover,
                        onVolumeUp = viewModel::volumeUp,
                        onVolumeDown = viewModel::volumeDown,
                        onChannelUp = viewModel::channelUp,
                        onChannelDown = viewModel::channelDown,
                        onToggleMute = viewModel::toggleMute,
                        onRewind = viewModel::rewind,
                        onPlayPause = viewModel::playPause,
                        onFastForward = viewModel::fastForward,
                        onNav = viewModel::nav,
                        apps = apps,
                        appsLoading = appsLoading,
                        onLaunchApp = viewModel::launchApp,
                        inputs = inputs,
                        onLoadInputs = viewModel::loadInputs,
                        onSelectInput = { viewModel.setInput(it.id) },
                        onCursorTouchStart = viewModel::cursorTouchStart,
                        onCursorMove = viewModel::cursorMove,
                        onCursorClick = viewModel::cursorClick,
                        onOpenTvSettings = viewModel::openTvSettings,
                        nowPlaying = nowPlaying,
                        onStop = viewModel::stop,
                        onPowerOff = viewModel::powerOff,
                        tvAddress = tvAddress,
                        hapticsEnabled = hapticsEnabled,
                        onForgetTv = viewModel::forgetTv,
                        onSetHaptics = viewModel::setHaptics,
                        onResetHints = viewModel::resetHints,
                        appTheme = appTheme,
                        themeMode = themeMode,
                        onSelectTheme = viewModel::setAppTheme,
                        onSelectThemeMode = viewModel::setThemeMode,
                        showGestureHint = showGestureHint,
                        onDismissGestureHint = viewModel::dismissGestureHint,
                        alwaysOn = alwaysOn,
                        onSetAlwaysOn = viewModel::setAlwaysOn,
                        modifier = Modifier
                            .padding(padding)
                            .trackInteractions { interactions.tryEmit(Unit) },
                    )
                    }
                }
            }
        }
    }

    /** Reconnect instantly whenever the app returns to the foreground (008 seamless reconnect). */
    override fun onStart() {
        super.onStart()
        viewModel.onForegrounded()
    }

    /** Drive TV volume from the phone's hardware rocker while connected (US2 #4). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.uiState.value.connection is ConnectionState.Connected) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { viewModel.volumeUp(); return true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { viewModel.volumeDown(); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
