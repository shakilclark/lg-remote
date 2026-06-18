---
description: "Task list for native Android LG webOS remote (direct-LAN)"
---

# Tasks: Native Android LG webOS Remote (Direct-LAN)

**Input**: Design documents from `specs/002-native-android-remote/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ssap-protocol.md

**Tests**: INCLUDED. The plan specifies JUnit5 + MockWebServer (SSAP framing, command mapping,
state machine, cursor math) + Compose UI tests, and `001` set the precedent. Pure protocol/logic
is testable without a device; final behaviour is verified on the real TV per slice.

**Base package**: `com.shakilclark.lgremote` under `android/app/src/main/java/.../` (tests under
`android/app/src/test/...` and `androidTest/...`). **Real TV**: `192.168.0.9` (manual IP, v1).

## Format: `[ID] [P?] [Story] Description`
- **[P]** = parallelizable (different files, no dependency).

---

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Create `android/` Gradle project: `settings.gradle.kts`, root `build.gradle.kts`,
  `gradle/libs.versions.toml`, `app/build.gradle.kts` (Kotlin 2.0, JVM 17, `minSdk 31`,
  `targetSdk` current, Compose BOM, Material 3).
- [x] T002 [P] Add dependencies to the version catalog + `app`: OkHttp, kotlinx-serialization-json,
  kotlinx-coroutines, AndroidX DataStore-preferences, Lifecycle (ViewModel/runtime-compose),
  Compose UI/Material3; test: JUnit5, kotlin-test, OkHttp MockWebServer, coroutines-test,
  Compose UI test.
- [x] T003 [P] `app/src/main/AndroidManifest.xml`: `INTERNET`, `ACCESS_NETWORK_STATE`; declare
  `MainActivity` (portrait, full-screen); app label/icon placeholder.
- [x] T004 [P] Configure ktlint/spotless + a `.editorconfig`; confirm `./gradlew :app:assembleDebug`
  builds an empty app.

**Checkpoint**: empty app builds and installs to the phone.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: every user story depends on these. No story work begins until this passes.

- [x] T005 [P] `tv/SsapMessages.kt` — `@Serializable` models: register request/response,
  generic request `{id,type,uri,payload}`, response/error, subscription envelope
  (per contracts/ssap-protocol.md).
- [x] T006 [P] `data/TvStore.kt` — DataStore(Preferences): persist `TVConnection` (id, name,
  address, clientKey) + `activeId`; never log clientKey (data-model.md).
- [x] T007 [P] `connection/ConnectionState.kt` — sealed `ConnectionState`
  (NeedsPairing/Connecting/Connected/Disconnected/OffNetwork/PermissionRequired).
- [x] T008 `tv/TvTrustManager.kt` — custom `X509TrustManager` + `HostnameVerifier` accepting the
  TV's self-signed cert; build the shared `OkHttpClient` (FR-016, R1).
- [x] T009 `tv/SsapClient.kt` — OkHttp WebSocket to `wss://<ip>:3001`; connect/close, send request
  with unique `id`, correlate responses, support subscribe; expose inbound as a `Flow`. Uses the
  T008 client.
- [x] T010 [P] `tv/SsapClientTest.kt` (MockWebServer) — request→outgoing JSON; response routed to
  the right pending `id`; error→failure; subscription emits. (Write to fail first.)
- [x] T011 `connection/ConnectionStateMachine.kt` — drive transitions from SsapClient events +
  reachability; auto-reconnect with backoff; refresh address on retry (FR-010, data-model.md).
- [x] T012 [P] `connection/ConnectionStateMachineTest.kt` — transition coverage incl. off-network
  and command-while-disconnected rejection.
- [x] T013 `RemoteViewModel.kt` (skeleton) — holds `StateFlow<UiState>` from the state machine +
  store; `MainActivity.kt` — full-screen `enableEdgeToEdge`, Compose host, app theme in
  `ui/theme/`.

**Checkpoint**: app can construct a client + state machine; tests green; nothing TV-facing yet.

---

## Phase 3: User Story 1 — Pair with my TV once (P1) 🎯 MVP

**Goal**: enter the TV's IP → accept the on-TV prompt → "Connected"; reopen → silent reconnect.
**Independent Test**: fresh install → pair → kill+reopen reconnects with no prompt.

- [x] T014 [P] [US1] `tv/Pairing.kt` — `register` handshake with `pairingType:PROMPT` + stored
  client-key; parse `registered`/prompt; return client-key to persist (R3, contract).
- [x] T015 [P] [US1] `tv/PairingTest.kt` (MockWebServer) — with-key (no prompt) and without-key
  (prompt→registered) paths; client-key persisted via T006.
- [x] T016 [US1] Wire pairing into `ConnectionStateMachine` + `RemoteViewModel` (NeedsPairing /
  Connecting / Connected); persist + reuse client-key on launch (FR-001/FR-002).
- [x] T017 [P] [US1] `ui/ConnectScreen.kt` — manual IP entry + "Connect"; shows "accept on your TV"
  during NeedsPairing.
- [x] T018 [P] [US1] `ui/ConnectionBanner.kt` — connection-state chip (FR-009) shown on every screen.
- [x] T019 [US1] `app` first-run flow in `MainActivity`/nav: no active TV → ConnectScreen; active TV
  → auto-connect on resume; re-establish socket + accurate state after background/kill.
- [x] T020 [P] [US1] `ui/ConnectScreenTest.kt` (Compose) — IP entry enables Connect; NeedsPairing
  copy renders.

**Checkpoint / DEMO on TV (192.168.0.9)**: pair fresh, accept on TV, see Connected; force-quit &
reopen → silent reconnect. Update quickstart.md validation log.

---

## Phase 4: User Story 2 — Control playback and volume (P1)

**Goal**: volume ±, mute, play/pause; hardware volume rocker drives the TV.
**Independent Test**: each control changes the TV and the app reflects volume/mute.

- [ ] T021 [P] [US2] `tv/Commands.kt` — volumeUp/volumeDown/setMute/play/pause + `playPause` toggle
  (track last media state, default pause); subscribe `getVolume`, parse **flat AND nested
  `volumeStatus`** shapes (R2, contract).
- [ ] T022 [P] [US2] `tv/CommandsTest.kt` (MockWebServer) — each command → correct URI/payload;
  both getVolume shapes parse to `(volume, muted)`.
- [ ] T023 [US2] Expose volume/mute/playPause through `RemoteViewModel`; feed live volume/mute from
  the subscription into `Connected` state.
- [ ] T024 [P] [US2] `ui/VolumePad.kt` + `ui/PlaybackBar.kt` — large vol±, mute (active style),
  play/pause; show current level; haptic on press (FR-012).
- [ ] T025 [US2] `MainActivity.onKeyDown` — capture `VOLUME_UP/DOWN` while foregrounded → TV volume,
  consume the event (US2 #4).
- [ ] T026 [US2] Reject controls when not Connected with a visible reason (FR-014).

**Checkpoint / DEMO on TV**: vol/mute/play-pause + hardware rocker all affect the TV; app stays in
sync. Update validation log.

---

## Phase 5: User Story 3 — Navigate the TV menus (P2)

**Goal**: D-pad + OK/Back/Home via the pointer-input socket.
**Independent Test**: arrows move focus, OK selects, Back/Home work.

- [ ] T027 [P] [US3] `tv/PointerSocket.kt` — request `getPointerInputSocket`, open the secondary
  `wss://` socket (reuse TrustManager), send `button` frames (UP/DOWN/LEFT/RIGHT/ENTER/BACK/HOME/
  EXIT) in the newline format (R4, contract).
- [ ] T028 [P] [US3] `tv/PointerSocketTest.kt` (MockWebServer) — getPointerInputSocket → second
  socket opened; button frames match the byte format.
- [ ] T029 [US3] Add `Nav` command path to `RemoteViewModel`; keep the pointer socket open while
  Connected, reopen on drop.
- [ ] T030 [P] [US3] `ui/DPad.kt` — directional pad + OK + Back/Home/Exit row; haptics.

**Checkpoint / DEMO on TV**: navigate webOS menus end-to-end. Update validation log.

---

## Phase 6: User Story 6 — App shortcuts (YouTube & Netflix) (P2)

**Goal**: one-tap YouTube / Netflix launch.
**Independent Test**: each shortcut launches its app; not-installed → clear message.

- [ ] T031 [P] [US6] Extend `tv/Commands.kt` — `launchApp(appKey)`: resolve id from
  `listLaunchPoints` titles, fallback `youtube.leanback.v4`/`netflix`; detect not-installed (R2,
  contract, US6 #3).
- [ ] T032 [P] [US6] `tv/LaunchAppTest.kt` (MockWebServer) — title→id resolution, fallback id,
  not-installed result.
- [ ] T033 [US6] Wire `LaunchApp` through `RemoteViewModel`; surface not-installed message.
- [ ] T034 [P] [US6] `ui/AppShortcuts.kt` — YouTube + Netflix buttons (branded).

**Checkpoint / DEMO on TV**: both shortcuts launch; not-installed handled. Update validation log.

---

## Phase 7: User Story 7 — Input switcher (P2)

**Goal**: list + switch external inputs.
**Independent Test**: input list shows labels; tapping one switches the TV source.

- [ ] T035 [P] [US7] Extend `tv/Commands.kt` — `listInputs` (`getExternalInputList`) → `TVInput`s;
  `setInput(inputId)` (`switchInput`); track active input (contract).
- [ ] T036 [P] [US7] `tv/InputsTest.kt` (MockWebServer) — list parses id+label; switchInput payload.
- [ ] T037 [US7] Wire inputs into `RemoteViewModel` (load on demand; reflect active).
- [ ] T038 [P] [US7] `ui/InputSwitcher.kt` — input chips with labels (e.g. HDMI_2 "PS4…"),
  active highlighted.

**Checkpoint / DEMO on TV**: see inputs, switch source, active reflected. Update validation log.

---

## Phase 8: User Story 5 — Motion (Magic Remote) cursor (P3)

**Goal**: hold-and-move drives the LG pointer; tap clicks; release parks it.
**Independent Test**: pointer tracks phone motion while held; tap clicks; no drift on release.

- [ ] T039 [P] [US5] `cursor/MotionCursor.kt` — register `TYPE_GAME_ROTATION_VECTOR` (fallback
  gyroscope) only while held; angular delta → `dx/dy` with dead-zone + low-pass smoothing;
  unregister on release (R5; start SENSITIVITY≈14, DEAD_ZONE≈0.25).
- [ ] T040 [P] [US5] `cursor/MotionCursorTest.kt` — dead-zone suppresses jitter; smoothing;
  no output when not held.
- [ ] T041 [US5] Extend `tv/PointerSocket.kt` — `move(dx,dy)` + `click()` frames; route MotionCursor
  output while Connected + held (reuses the US3 socket).
- [ ] T042 [P] [US5] `ui/CursorPad.kt` — hold-to-move button (active style) + Click; haptic on press.

**Checkpoint / DEMO on TV**: cursor tracks motion, clicks, no drift on release; retune sensitivity
on-device (SC-007). Update validation log.

---

## Phase 9: Polish & Cross-Cutting

- [ ] T043 [P] Off-network detection (phone leaves Wi-Fi) → `OffNetwork` state + recovery copy
  (edge case, FR-009).
- [ ] T044 [P] App icon + full-screen/edge-to-edge polish; portrait lock; consistent theme/haptics.
- [ ] T045 [P] `ui/RemoteScreenTest.kt` (Compose) — Connected layout shows volume/dpad/cursor/
  shortcuts/inputs; Disconnected shows reconnect affordance.
- [ ] T046 Release signing: local keystore + `assembleRelease`; document `adb install` in
  quickstart.md (FR-011).
- [ ] T047 Run the full quickstart.md validation log on the real TV; fix any gaps.
- [ ] T048 [P] (Deferred) SSDP discovery slice — `WifiManager.MulticastLock` + M-SEARCH + multi-TV
  picker (FR-003 auto, FR-015); add `CHANGE_WIFI_MULTICAST_STATE`. Tracked, not MVP.
- [ ] T049 [P] (Deferred) `ACCESS_LOCAL_NETWORK` runtime permission + rationale UI + denied path,
  when bumping to `targetSdk 37+` (FR-017).

---

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2, BLOCKS all stories)** → user stories.
- Story order (demo cadence, per plan): **US1 → US2 → US3 → US6 → US7 → US5**, each an independent,
  TV-demoable increment. US6/US7/US5 build on the Commands/PointerSocket foundations from US2/US3.
- **Polish (P9)** after the desired stories; T048/T049 are explicitly deferred.
- Within a story: write the MockWebServer/unit test (fails first) → implement protocol → wire
  ViewModel → build Compose UI → **demo on the real TV** → commit.

### Parallel opportunities
- T002/T003/T004 setup; T005/T006/T007 foundational models; per-story `[P]` tasks touch different
  files (protocol vs test vs Compose) and can overlap.

## Implementation Strategy
- **MVP = Phases 1–4** (Setup + Foundational + US1 pair + US2 volume/playback): a usable everyday
  remote. STOP and validate on the TV before continuing.
- Then add US3, US6, US7, US5 as independent slices, demoing each.
- Commit after each task/logical group; keep the tree runnable; push after each completed slice.
