# Feature Specification: LG webOS TV Remote (App-like Phone Remote)

**Feature Branch**: `001-webos-remote`

**Created**: 2026-06-18

**Status**: Draft

**Input**: User description: "A remote control app to interface with an LG webOS smart TV, installable on my phone, with no fuss. Doesn't have to be a PWA — best choice recommended."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pair with my TV once (Priority: P1)

The first time I open the app, it finds (or lets me point it at) my LG TV on my home
network and pairs with it. The TV shows a one-time "allow this device?" prompt; I accept
on the TV, and from then on the app reconnects automatically without asking again.

**Why this priority**: Nothing else works until the app can talk to the TV. A remembered
pairing is the foundation of the "zero-fuss" promise — it must happen exactly once.

**Independent Test**: From a device that has never connected before, open the app,
complete pairing by accepting the prompt on the TV, then close and reopen the app and
confirm it reconnects silently. Delivers value on its own: proof the app can reach the TV.

**Acceptance Scenarios**:

1. **Given** the app has never been paired and the TV is on the same network, **When** I
   open the app, **Then** I am guided to connect and the TV displays a pairing prompt.
2. **Given** I accept the prompt on the TV, **When** pairing completes, **Then** the app
   shows a "connected" state and remembers the TV for next time.
3. **Given** I have paired before, **When** I reopen the app, **Then** it reconnects
   automatically without showing a pairing prompt.

---

### User Story 2 - Control playback and volume (Priority: P1)

Once connected, I can do the everyday things a physical remote does: change the volume,
mute, and play/pause whatever is on screen — with large, reliable touch buttons.

**Why this priority**: These are the highest-frequency actions; they justify reaching for
the app instead of hunting for the physical remote.

**Independent Test**: With a paired TV, press volume-up/down, mute, and play/pause and
observe the TV respond within a moment. Delivers a usable everyday remote on its own.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I tap volume-up, **Then** the TV volume increases
   and the new level is reflected in the app.
2. **Given** a connected TV, **When** I tap mute, **Then** the TV mutes/unmutes and the
   app shows the current mute state.
3. **Given** media is playing, **When** I tap play/pause, **Then** playback toggles on the
   TV.

---

### User Story 3 - Navigate the TV menus (Priority: P2)

I can drive the on-screen interface: directional pad (up/down/left/right), OK/select,
back, and home — so I can move around webOS, pick inputs, and confirm choices.

**Why this priority**: Navigation rounds out the "real remote" experience and is needed to
reach anything the dedicated buttons don't cover, but volume/playback (P1) already deliver
a usable MVP.

**Independent Test**: With a connected TV, use the d-pad and OK to move the on-screen
highlight and select an item; use back/home to return. Verifiable independently.

**Acceptance Scenarios**:

1. **Given** a connected TV showing a menu, **When** I tap a direction, **Then** the
   on-screen focus moves accordingly.
2. **Given** a focused item, **When** I tap OK, **Then** the item is selected/activated.
3. **Given** any screen, **When** I tap home, **Then** the TV returns to the webOS home.

---

### User Story 4 - Launch apps and switch inputs (Priority: P3)

I can see the apps installed on the TV (e.g. streaming apps) and the available inputs
(HDMI, etc.) and jump straight to one with a single tap.

**Why this priority**: A convenience that beats the physical remote, but not required for
a viable first version.

**Independent Test**: With a connected TV, open the app list, tap an app, and confirm it
launches on the TV; switch to an HDMI input and confirm the source changes.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I open the app launcher, **Then** I see the TV's
   installed apps.
2. **Given** the app launcher, **When** I tap an app, **Then** that app opens on the TV.
3. **Given** the input list, **When** I tap an input, **Then** the TV switches to it.

---

### Edge Cases

- **TV is off or asleep**: the app must clearly show it cannot reach the TV and offer to
  retry, rather than appearing to send commands into the void. (Power-on / Wake-on-LAN is
  out of scope for v1 — see Assumptions.)
- **TV's network address changed** (new DHCP lease): the app should recover the connection
  without forcing the user to re-pair.
- **Pairing prompt declined or timed out on the TV**: the app explains what happened and
  lets the user try again.
- **Network change** (phone leaves Wi-Fi): the app detects it is off-network and surfaces
  a clear "not on the same network as your TV" state.
- **Multiple LG TVs on the network**: the app must let the user pick which one (and not
  silently connect to the wrong one).
- **A command is sent while reconnecting**: the action is either queued briefly or clearly
  rejected — never silently dropped.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST connect to an LG webOS TV on the user's local network and
  complete the TV's one-time on-device pairing/authorization flow.
- **FR-002**: The app MUST persist the TV's identity and pairing credential so that
  subsequent launches reconnect automatically without re-pairing.
- **FR-003**: Users MUST be able to locate their TV either by automatic discovery on the
  network or by entering the TV's network address manually as a fallback.
- **FR-004**: The app MUST let the user control volume up, volume down, and mute, and
  reflect the resulting volume/mute state.
- **FR-005**: The app MUST let the user toggle media play/pause.
- **FR-006**: The app MUST provide directional navigation (up/down/left/right), OK/select,
  back, and home controls.
- **FR-007**: The app MUST list the TV's installed apps and launch a selected app on the
  TV. *(P3 — may ship after MVP)*
- **FR-008**: The app MUST list available inputs/sources and switch to a selected one.
  *(P3 — may ship after MVP)*
- **FR-009**: The app MUST always display the current connection state (e.g. connected,
  connecting, disconnected, needs-pairing, off-network) to the user.
- **FR-010**: The app MUST automatically attempt to reconnect when a previously paired TV
  becomes reachable again, including after the TV's network address changes.
- **FR-011**: The app MUST be installable to a phone's home screen and launch in a
  standalone, app-like, full-screen manner (no browser address bar). The specific
  installation mechanism is an implementation choice to be decided at planning.
- **FR-012**: The app MUST present a touch-first interface usable one-handed in portrait
  orientation, with controls large enough to operate without looking closely.
- **FR-013**: The app MUST NOT transmit TV control traffic, the pairing credential, or
  usage data to any third-party/cloud service; all control stays within the user's own
  network or devices.
- **FR-014**: When the TV is unreachable, the app MUST visibly indicate failure and offer
  retry, rather than silently discarding control actions.
- **FR-015**: When more than one compatible TV is discovered, the app MUST let the user
  choose which TV to control.

### Key Entities *(include if feature involves data)*

- **TV Connection**: a remembered television the app controls. Attributes: friendly name,
  network address (which may change over time), and the pairing credential proving this
  device is authorized. At most a small number are remembered; one is "active" at a time.
- **Control Command**: a single user-initiated action sent to the TV (e.g. volume-up,
  play/pause, navigate-left, launch-app, switch-input). Not persisted; it has a target TV,
  a type, optional parameters, and a result (acknowledged / failed).
- **Connection State**: the app's current understanding of its link to the active TV
  (needs-pairing, connecting, connected, disconnected, off-network), surfaced in the UI.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can go from opening the app to a paired, connected TV in
  under 60 seconds (excluding any unavoidable on-TV prompt wait).
- **SC-002**: On a previously paired device, reopening the app reaches the "connected"
  state automatically in under 5 seconds when the TV is on and on-network, with no taps.
- **SC-003**: A control action (e.g. volume change) is reflected on the TV within 1 second
  of the tap, on a typical home network.
- **SC-004**: The app is installable to the home screen on the user's Android phone and
  iPad and launches full-screen with no browser address bar.
- **SC-005**: In 100% of cases where the TV is off or off-network, the app shows an
  explicit disconnected/off-network state rather than appearing to succeed.
- **SC-006**: A returning user can perform the three most common actions (volume, mute,
  play/pause) within 2 taps of launching the app.

## Assumptions

- **Same network or private overlay**: the phone reaches the TV either by being on the
  same local network, or via a private network overlay (Tailscale) that links the user's
  devices to the home; away-from-home control is in scope (see SC-007).
- **TV is powered/standby-reachable**: turning a fully-off TV on (e.g. Wake-on-LAN) is out
  of scope for v1; the app controls a TV that is on or in a network-reachable standby.
- **Single primary TV**: the typical user controls one TV; multi-TV discovery and
  selection is supported (FR-015) but switching between many TVs is a lightweight picker,
  not a management surface.
- **No accounts**: there is no login, user account, or server-side user data; the only
  stored state is TV connection details and the pairing credential.
- **LG webOS only**: the TV runs a webOS version that exposes LG's standard local control
  protocol; other brands and pre-webOS LG sets are out of scope.
- **Free tooling**: the solution favours free, open tools and the user's existing hardware
  (an always-on Linux laptop, a MacBook Pro, an Android phone, and an iPad).
- **English-only UI** for v1; localization is out of scope.

## Clarifications

### Session 2026-06-18

- **MVP control scope**: The first shippable version covers User Stories 1–3 (P1+P2):
  pairing, volume/mute, play/pause, and full menu navigation (d-pad, OK, back, home).
  User Story 4 (app launcher + input switching, P3 / FR-007, FR-008) is deferred to a
  follow-up slice.
- **Away-from-home access**: In scope. The remote works on the home network and from
  outside the home, achieved via a private network overlay (Tailscale) connecting the
  user's devices — not a third-party cloud control service (preserves FR-013).
  Consequently **SC-007** is added below.
- **Test target**: A real LG webOS TV is available on the user's network; development and
  verification are performed directly against it.
- **Host**: The always-on agent runs on a **Raspberry Pi** (preferred over the Raspberry Pi:
  low-power, silent, always-on). It must sit on the same LAN as the TV.
- **Sharing model (B)**: Personal use plus optional sharing with **trusted people invited
  onto the user's private overlay (Tailscale)** to control the user's own TV. No accounts,
  no third-party control cloud (preserves FR-013). A future multi-tenant service (each user
  controlling their own TV) is explicitly deferred; the local agent is kept separable so it
  could be reused under a cloud-relay layer later.

Additional success criterion arising from these clarifications:

- **SC-007**: With the private network overlay active, the remote can connect to and
  control the TV from a phone that is off the home Wi-Fi (e.g. on mobile data), with the
  same connection-state feedback as on the home network.
