package com.shakilclark.lgremote.connection

/**
 * The app's understanding of its link to the active TV (FR-009, data-model.md). Always
 * surfaced in the UI; commands are only accepted while [Connected].
 */
sealed interface ConnectionState {
    /** TV is showing the on-screen accept prompt. */
    data class NeedsPairing(val message: String = "Accept the prompt on your TV") : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val volume: Int? = null, val muted: Boolean? = null) : ConnectionState
    data class Disconnected(val message: String = "Can't reach your TV") : ConnectionState
    data class OffNetwork(val message: String = "Not on the same network as your TV") : ConnectionState
    /** Android local-network permission denied (FR-017). */
    data object PermissionRequired : ConnectionState
}
