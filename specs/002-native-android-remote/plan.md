# Implementation Plan: Native Android LG webOS Remote (Direct-LAN)

**Branch**: `002-native-android-remote` | **Date**: 2026-06-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-native-android-remote/spec.md`

## Summary

Re-platform the LG webOS remote as a **native Android app** (Kotlin + Jetpack Compose) that
talks **SSAP directly to the TV over the LAN** — no backend, no Tailscale, no HTTPS. The app
opens the TV's secure control socket (`wss://<tv-ip>:3001`), trusting the TV's self-signed
certificate via a custom `TrustManager` (the one thing a browser can't do, and the reason the
`001` server tier existed). A hand-written Kotlin SSAP client handles pairing (client-key),
command URIs, and the secondary pointer-input socket; a `SensorManager`-driven motion cursor
reuses that pointer socket. UI is built directly in Compose. Delivered in demonstrable slices
against the real TV (192.168.0.9), same cadence as `001`.

## Technical Context

**Language/Version**: Kotlin 2.0.x, JVM target 17

**Primary Dependencies**:
- Jetpack Compose (BOM 2024.x) + Material 3 — UI
- OkHttp 4.12.x — WebSocket transport (SSAP + pointer socket) with a custom `TrustManager`
- kotlinx.serialization (JSON) — SSAP message (de)serialization
- kotlinx.coroutines + `Flow`/`StateFlow` — async + reactive connection state
- AndroidX DataStore (Preferences) — persist TV identity + client-key (app-private)
- AndroidX Lifecycle (ViewModel, lifecycle-aware collection)
- (No third-party SSAP/webOS library — client written from scratch; Connect-SDK &
  `heroslender/lg-remote` are read-only references.)

**Storage**: AndroidX DataStore in app-private storage (`filesDir`); holds `TVConnection`
records (name, last-known address, client-key) and the active TV id. Client-key never leaves
the device.

**Testing**: JUnit5 + kotlin-test for pure logic (SSAP framing, command→URI mapping, state
machine, cursor smoothing/dead-zone); MockWebServer (OkHttp) as a mock webOS socket for the
client; Compose UI tests (`createComposeRule`) for key screens. Manual verification on the
real phone + TV per slice (Principle V).

**Target Platform**: Android, `minSdk 31` (Android 12), `targetSdk` current (35/36).

**Project Type**: Single native Android app (Gradle, one `:app` module to start; protocol
logic isolated in packages so it could become a module later).

**Performance Goals**: Control action reflected on TV < 1s (SC-003); reconnect < 5s (SC-002);
motion cursor end-to-end latency ~100ms while held (SC-007); UI at 60fps.

**Constraints**: Home-LAN only; no backend/cloud in control path; secure socket only
(`wss://:3001`, no cleartext `:3000`); Android 17/targetSDK 37+ runtime local-network
permission must be requested and degrade gracefully if denied.

**Scale/Scope**: Single user, 1 primary TV (small remembered set), ~5 screens/surfaces
(connect/pair, remote home, cursor, app-shortcuts, input switcher). Sideloaded APK.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Zero-Fuss Operation | ✅ PASS | Pair once (client-key persisted); cert trusted silently in code, never shown to user; launch = open the app. |
| II. Installable & Phone-First | ✅ PASS | Native Android APK, full-screen, portrait, touch-first, sideloaded (no store account). |
| III. Local-Network & Private | ✅ PASS (strengthened) | Direct device→TV; no backend/relay/cloud at all; client-key only in app-private DataStore. |
| IV. Resilient Connectivity | ✅ PASS | Explicit `ConnectionState` (`StateFlow`) surfaced in UI; auto-reconnect; IP-change recovery; off-network + permission-denied states; no silent drops. |
| V. Demonstrable Vertical Slices | ✅ PASS | Slices map to US1→US2→US3→(US6/US7)→US5, each demoable on the real TV. |

**Result**: PASS — no violations. Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/002-native-android-remote/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions (transport, cert trust, sensors, structure)
├── data-model.md        # Phase 1 — entities & state machine
├── quickstart.md        # Phase 1 — build/run/sideload + per-slice TV validation log
├── contracts/
│   └── ssap-protocol.md # Phase 1 — native SSAP contract (handshake, URIs, pointer socket)
└── tasks.md             # Phase 2 — created by /speckit-tasks (NOT here)
```

### Source Code (repository root)

The native app lives under `android/` (the `001` `backend/` + `frontend/` stay as reference).

```text
android/
├── settings.gradle.kts
├── build.gradle.kts                # root
├── gradle/libs.versions.toml       # version catalog
└── app/
    ├── build.gradle.kts            # minSdk 31, Compose, OkHttp, serialization, DataStore
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml # INTERNET, ACCESS_NETWORK_STATE, (CHANGE_)WIFI_MULTICAST_STATE later; ACCESS_LOCAL_NETWORK for targetSDK 37+
        │   └── java/com/shakilclark/lgremote/
        │       ├── tv/             # SSAP client (from scratch)
        │       │   ├── SsapClient.kt          # OkHttp WS to wss://ip:3001, framing, request/response + subscriptions
        │       │   ├── TvTrustManager.kt      # trust the TV's self-signed cert
        │       │   ├── Pairing.kt             # hello → register(PROMPT) → client-key
        │       │   ├── PointerSocket.kt       # getPointerInputSocket → button/move/click
        │       │   ├── Commands.kt            # volume/mute/playPause/nav/launchApp/setInput
        │       │   └── SsapMessages.kt        # @Serializable request/response models
        │       ├── connection/     # ConnectionStateMachine, reconnection, ConnectionState
        │       ├── data/           # DataStore: TvStore (TVConnection records, active id, client-key)
        │       ├── cursor/         # MotionCursor: SensorManager → dx/dy (smoothing, dead-zone, hold-gate)
        │       ├── ui/             # Compose: theme/, RemoteScreen, ConnectScreen, components (VolumePad, DPad, CursorPad, AppShortcuts, InputSwitcher, ConnectionBanner)
        │       ├── RemoteViewModel.kt          # exposes StateFlow<UiState>, dispatches commands
        │       └── MainActivity.kt             # full-screen, hardware volume-key capture, permission flow
        ├── test/                   # JUnit5 + MockWebServer: framing, command mapping, state machine, cursor math
        └── androidTest/            # Compose UI tests
```

**Structure Decision**: New top-level `android/` Gradle project, single `:app` module. SSAP
protocol code is isolated under `tv/` (pure-ish, MockWebServer-testable) so the networking is
verifiable without a device and could later be extracted into its own module. The `001`
`backend/` (proven `lgtv2` behaviour) and `frontend/` (visual reference) are retained untouched
as references, not built.

## Complexity Tracking

> No constitution violations — section intentionally empty.
