# Feature Specification: Native Android LG webOS Remote (Direct-LAN)

**Feature Branch**: `002-native-android-remote`

**Created**: 2026-06-18

**Status**: Draft

**Supersedes**: `001-webos-remote` (PWA + Node backend + Tailscale). This re-platforms the
same remote as a **native Android app that talks directly to the TV over the LAN**, removing
the backend, Tailscale, and HTTPS entirely.

**Input**: User description: "I want a native Android app to control my LG webOS TV. Don't
optimise for least-effort — make it a good app. Android-only and home-Wi-Fi-only are fine."

## Why re-platform (context)

The `001` architecture needed a Node backend + Tailscale HTTPS for one reason: a browser/PWA
cannot open an insecure socket to the TV (mixed-content), and the TV's secure socket uses an
untrusted self-signed certificate a browser refuses. A **native Android app is not bound by
browser certificate rules** — it can connect to the TV's secure SSAP socket and trust the
self-signed cert programmatically. That single capability deletes the entire server tier:
no backend, no Tailscale, no HTTPS, no always-on host. The TV-side protocol (pairing,
commands, pointer cursor) is unchanged from `001`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pair with my TV once (Priority: P1)

The first time I open the app, it finds (or lets me point it at) my LG TV on my home
network and pairs with it. The TV shows a one-time "allow this device?" prompt; I accept on
the TV, and from then on the app reconnects automatically without asking again.

**Why this priority**: Nothing else works until the app can talk to the TV. A remembered
pairing is the foundation of the "zero-fuss" promise — it must happen exactly once.

**Independent Test**: On a fresh install, open the app, complete pairing by accepting the
prompt on the TV, then force-quit and reopen the app and confirm it reconnects silently.

**Acceptance Scenarios**:

1. **Given** the app has never been paired and the TV is on the same Wi-Fi, **When** I open
   the app, **Then** I am guided to connect and the TV displays a pairing prompt.
2. **Given** I accept the prompt on the TV, **When** pairing completes, **Then** the app
   shows a "connected" state and remembers the TV for next time.
3. **Given** I have paired before, **When** I reopen the app, **Then** it reconnects
   automatically without showing a pairing prompt.
4. **Given** the TV presents a self-signed certificate, **When** the app connects, **Then**
   it trusts that certificate silently in code and the user is never shown a cert warning.

---

### User Story 2 - Control playback and volume (Priority: P1)

Once connected, I can do the everyday things a physical remote does: change the volume,
mute, and play/pause whatever is on screen — with large, reliable touch buttons.

**Why this priority**: These are the highest-frequency actions; they justify reaching for
the app instead of the physical remote.

**Independent Test**: With a paired TV, press volume-up/down, mute, and play/pause and
observe the TV respond within a moment.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I tap volume-up, **Then** the TV volume increases and
   the new level is reflected in the app.
2. **Given** a connected TV, **When** I tap mute, **Then** the TV mutes/unmutes and the app
   shows the current mute state.
3. **Given** media is playing, **When** I tap play/pause, **Then** playback toggles on the TV.
4. **Given** the phone's hardware volume rocker is pressed while the app is foregrounded,
   **When** I press it, **Then** the TV volume changes (native-only capability; see notes).

---

### User Story 3 - Navigate the TV menus (Priority: P2)

I can drive the on-screen interface: directional pad (up/down/left/right), OK/select, back,
and home — so I can move around webOS, pick inputs, and confirm choices.

**Why this priority**: Navigation rounds out the "real remote" experience and is needed to
reach anything the dedicated buttons don't cover, but volume/playback (P1) already deliver a
usable MVP.

**Independent Test**: With a connected TV, use the d-pad and OK to move the on-screen
highlight and select an item; use back/home to return.

**Acceptance Scenarios**:

1. **Given** a connected TV showing a menu, **When** I tap a direction, **Then** the
   on-screen focus moves accordingly.
2. **Given** a focused item, **When** I tap OK, **Then** the item is selected/activated.
3. **Given** any screen, **When** I tap home, **Then** the TV returns to the webOS home.

---

### User Story 4 - Launch apps and switch inputs (Priority: P3)

I can see the apps installed on the TV (e.g. streaming apps) and the available inputs (HDMI,
etc.) and jump straight to one with a single tap.

**Why this priority**: A convenience that beats the physical remote, but not required for a
viable first version. US6 and US7 below are the concrete, higher-value slices of this.

**Independent Test**: With a connected TV, open the app list, tap an app, confirm it
launches; switch to an HDMI input and confirm the source changes.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I open the app launcher, **Then** I see the TV's
   installed apps.
2. **Given** the app launcher, **When** I tap an app, **Then** that app opens on the TV.
3. **Given** the input list, **When** I tap an input, **Then** the TV switches to it.

---

### User Story 5 - Motion (Magic Remote) cursor (Priority: P3)

I can hold a button on the phone and **move/tilt the phone in the air** to drive the LG
on-screen pointer (like the Magic Remote), and tap to click. Releasing the button parks the
cursor so it doesn't drift.

**Why this priority**: A delightful power-feature that matches the native Magic Remote, but
the D-pad (US3) already covers navigation, so this is an enhancement, not MVP.

**Independent Test**: With a connected TV, hold the cursor button and rotate the phone — the
on-screen pointer tracks the motion; tap to activate the focused item; release to stop.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I hold the cursor button and move the phone, **Then**
   the LG pointer moves correspondingly on screen.
2. **Given** the cursor is showing, **When** I tap, **Then** the item under the pointer is
   clicked.
3. **Given** I release the cursor button, **When** I move the phone, **Then** the pointer
   does **not** move (input is gated to the held state).

**Notes / constraints** (for planning):
- Reuses the **US3 pointer-input socket**: `move {dx, dy}` and `click` frames (same socket as
  the buttons), so no new TV-side capability is needed.
- Phone motion uses Android's native **`SensorManager`** (rotation-vector / gyroscope). Unlike
  the `001` PWA, this needs **no HTTPS and no web permission prompt** — a key reason native is
  a better fit for this feature. Needs sensitivity/smoothing + a dead-zone so the pointer
  doesn't jitter; gate all motion strictly to "button held" to avoid drift.

---

### User Story 6 - App shortcuts (YouTube & Netflix) (Priority: P2)

I have dedicated one-tap buttons on the remote for **YouTube** and **Netflix** that launch
the app straight on the TV — the two I open most, without hunting through the TV's home.

**Why this priority**: High everyday value and small scope. A focused subset of the full app
launcher (US4): rather than a whole grid, just the two apps I actually use.

**Independent Test**: With a connected TV, tap the YouTube shortcut → YouTube opens; tap
Netflix → Netflix opens. Works regardless of what's currently on screen.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I tap the YouTube shortcut, **Then** YouTube launches
   (or foregrounds) on the TV.
2. **Given** a connected TV, **When** I tap the Netflix shortcut, **Then** Netflix launches.
3. **Given** an app isn't installed on the TV, **When** I tap its shortcut, **Then** the app
   tells me it couldn't launch rather than failing silently.

**Notes / constraints** (for planning):
- Launch via `ssap://system.launcher/launch` with the app id. Well-known webOS ids: Netflix =
  `netflix`, YouTube = `youtube.leanback.v4`. Ids vary by webOS version — resolve robustly by
  matching `listLaunchPoints` titles to the real id, falling back to the well-known id.

---

### User Story 7 - Input switcher (Priority: P2)

I can see the TV's inputs/sources (HDMI 1/2/…, etc.) and switch to one with a tap — e.g. jump
to the HDMI my console or soundbar is on.

**Why this priority**: A common everyday action the physical remote buries; small scope and
high utility. The second concrete slice of US4.

**Independent Test**: With a connected TV, open the input list → see the available sources →
tap one → the TV switches to that input.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I open the input switcher, **Then** I see the TV's
   available external inputs (with their labels where provided).
2. **Given** the input list, **When** I tap an input, **Then** the TV switches to that source.
3. **Given** the input list, **When** the active input changes, **Then** the app reflects
   which input is current.

**Notes / constraints** (for planning):
- List via `ssap://tv/getExternalInputList`; switch via `ssap://tv/switchInput` with
  `{ inputId }`. Use the device's labels/icons from the list where available.

---

### Edge Cases

- **TV is off or asleep**: the app must clearly show it cannot reach the TV and offer to
  retry, rather than appearing to send commands into the void. (Power-on / Wake-on-LAN is out
  of scope for v1.)
- **TV's network address changed** (new DHCP lease): the app should recover the connection
  without forcing the user to re-pair (re-discover by device identity).
- **Pairing prompt declined or timed out on the TV**: the app explains what happened and lets
  the user try again.
- **Phone leaves home Wi-Fi** (e.g. on mobile data, or a different network): the app detects
  it is off-network and surfaces a clear "not on the same network as your TV" state. Away-from-
  home control is explicitly out of scope.
- **Android local-network permission denied** (Android 17 / targetSDK 37+): the app explains
  that it needs local-network access to reach the TV and links to the setting, rather than
  silently failing discovery/connection.
- **Multiple LG TVs on the network**: the app must let the user pick which one (and not
  silently connect to the wrong one).
- **A command is sent while reconnecting**: the action is either queued briefly or clearly
  rejected — never silently dropped.
- **App backgrounded / process killed**: on return, the app re-establishes the socket and
  shows accurate connection state; it does not present stale "connected" UI.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST connect to an LG webOS TV on the user's local network and complete
  the TV's one-time on-device pairing/authorization flow.
- **FR-002**: The app MUST persist the TV's identity and pairing credential in private
  on-device storage so subsequent launches reconnect automatically without re-pairing.
- **FR-003**: Users MUST be able to locate their TV either by automatic discovery on the
  network or by entering the TV's network address manually as a fallback.
- **FR-004**: The app MUST let the user control volume up, volume down, and mute, and reflect
  the resulting volume/mute state.
- **FR-005**: The app MUST let the user toggle media play/pause.
- **FR-006**: The app MUST provide directional navigation (up/down/left/right), OK/select,
  back, and home controls.
- **FR-007**: The app MUST list the TV's installed apps and launch a selected app on the TV.
  *(P3 — may ship after MVP; US6 is the concrete near-term slice.)*
- **FR-008**: The app MUST list available inputs/sources and switch to a selected one.
  *(P2 — US7.)*
- **FR-009**: The app MUST always display the current connection state (connected,
  connecting, disconnected, needs-pairing, off-network) to the user.
- **FR-010**: The app MUST automatically attempt to reconnect when a previously paired TV
  becomes reachable again, including after the TV's network address changes.
- **FR-011**: The app MUST be a **native Android application** installed from the home screen
  and run full-screen (no browser, no PWA shell). It MUST be installable by **sideloading a
  signed APK** (e.g. `adb install`); an app-store account MUST NOT be required.
- **FR-012**: The app MUST present a touch-first interface usable one-handed in portrait
  orientation, with controls large enough to operate without looking closely. It SHOULD use
  native Android UX affordances (haptic feedback on press; hardware volume-rocker capture
  while foregrounded).
- **FR-013**: The app MUST control the TV **directly** over the local network with NO backend,
  relay, or cloud service in the control path, and MUST NOT transmit control traffic, the
  pairing credential, or usage data to any third party.
- **FR-014**: When the TV is unreachable, the app MUST visibly indicate failure and offer
  retry, rather than silently discarding control actions.
- **FR-015**: When more than one compatible TV is discovered, the app MUST let the user choose
  which TV to control.
- **FR-016**: The app MUST connect over the TV's **secure SSAP socket** and trust the TV's
  self-signed certificate programmatically; it MUST NOT depend on the deprecated cleartext
  socket, and MUST NOT require the user to act on any certificate warning.
- **FR-017**: Where the Android version requires it (Android 17 / targetSDK 37+), the app MUST
  request the runtime local-network permission and, if denied, surface a clear explanation
  and recovery path rather than failing silently (ties to FR-014).

### Key Entities *(include if feature involves data)*

- **TV Connection**: a remembered television the app controls. Attributes: friendly name,
  network address (which may change), and the pairing credential proving this device is
  authorized. A small number may be remembered; one is "active" at a time. Stored in the
  app's private on-device storage.
- **Control Command**: a single user-initiated action sent to the TV (volume-up, play/pause,
  navigate-left, launch-app, switch-input). Not persisted; has a target TV, a type, optional
  parameters, and a result (acknowledged / failed).
- **Connection State**: the app's current understanding of its link to the active TV
  (needs-pairing, connecting, connected, disconnected, off-network), surfaced in the UI.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can go from opening the app to a paired, connected TV in
  under 60 seconds (excluding the unavoidable on-TV prompt wait).
- **SC-002**: On a previously paired phone, reopening the app reaches the "connected" state
  automatically in under 5 seconds when the TV is on and on-network, with no taps.
- **SC-003**: A control action (e.g. volume change) is reflected on the TV within 1 second of
  the tap, on a typical home network.
- **SC-004**: The app installs as a native APK on the user's Android phone and launches
  full-screen with no browser chrome.
- **SC-005**: In 100% of cases where the TV is off or off-network, the app shows an explicit
  disconnected/off-network state rather than appearing to succeed.
- **SC-006**: A returning user can perform the three most common actions (volume, mute,
  play/pause) within 2 taps of launching the app.
- **SC-007**: The motion cursor responds to phone movement within ~100 ms of motion while the
  cursor button is held, and does not move when it is released.

## Assumptions

- **Same Wi-Fi only**: the phone reaches the TV by being on the same local network as the TV.
  Away-from-home control is **out of scope** (this is what previously required a server +
  tunnel; removing it is the point of this re-platform).
- **Android only**: the client is a native Android app. iOS/iPadOS and desktop are out of
  scope.
- **TV is powered/standby-reachable**: turning a fully-off TV on (Wake-on-LAN) is out of scope
  for v1; the app controls a TV that is on or in a network-reachable standby.
- **Single primary TV**: the typical user controls one TV; multi-TV discovery/selection is a
  lightweight picker (FR-015), not a management surface.
- **No accounts**: no login, user account, or server-side data; the only stored state is TV
  connection details and the pairing credential, on-device.
- **LG webOS only**: the TV runs a webOS version exposing LG's standard local control protocol
  (SSAP) on the secure socket; other brands and pre-webOS LG sets are out of scope.
- **Free tooling**: built with free, open tooling (Android Studio / Kotlin / Jetpack Compose),
  sideloaded to the user's own Android phone; no paid developer account required.
- **English-only UI** for v1; localization is out of scope.

## Clarifications

### Session 2026-06-18 (re-platform)

- **Platform**: Native **Android** (Kotlin + Jetpack Compose). iPad/iOS support from `001` is
  **dropped** (accepted by the user).
- **Connectivity scope**: **Home Wi-Fi / same-LAN only.** Away-from-home access and the
  Tailscale overlay from `001` are **removed** (accepted by the user). `001`'s SC-007 is
  retired.
- **Architecture**: **No backend.** The phone owns the only socket to the TV and talks SSAP
  directly. The Raspberry Pi host and Node service from `001` are removed.
- **TV transport**: connect to `wss://<tv-ip>:3001` (secure SSAP) and trust the TV's
  self-signed certificate via a custom trust manager in app code. The deprecated cleartext
  `ws://:3000` is not relied upon (LG deprecated it; modern firmware favours 3001).
- **Distribution**: sideload a signed APK (`adb install` or a build link). No Play Store
  account ($25) required.
- **Design approach**: the UI is (re)designed **directly in Jetpack Compose** with live
  previews — not ported from the `001` web UI (which serves only as a visual reference).
- **Reference implementations**: `heroslender/lg-remote` (100% Kotlin, actively maintained)
  and LG's archived `Connect-SDK-Android-Core` (exact SSAP URIs, pairing handshake, and
  pointer-socket wire format) may be used as references; the verified SSAP behaviour from the
  working `001` backend is the source of truth for protocol details.

### Resolved (Session 2026-06-18, /clarify)

- **C1 — Minimum Android version**: **`minSdk 31`** (Android 12, 2021+), `targetSdk` current.
  Modern-only keeps the permission/TLS story simple and avoids legacy handling.
- **C2 — SSAP client strategy**: **Write the SSAP client from scratch in Kotlin** using OkHttp
  WebSocket + a custom `TrustManager` for the TV's self-signed cert. No third-party SSAP
  dependency; the verified `001` backend behaviour is the protocol source of truth.
  `Connect-SDK` / `heroslender/lg-remote` remain read-only references only.
- **C3 — Discovery mechanism**: **Manual IP entry first** (the user's TV is at a known
  address); **SSDP auto-discovery is a later polish slice** (will need an Android multicast
  lock). FR-003's manual fallback is therefore the v1 primary path; FR-015 multi-TV picker
  follows once discovery lands.
