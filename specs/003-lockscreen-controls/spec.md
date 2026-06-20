# Feature Specification: Lockscreen Controls

**Feature Branch**: `003-lockscreen-controls`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the native Android app, its SSAP client,
`TvConnectionManager`, and `Commands`). This feature adds a way to drive the **already-paired,
already-connected** TV from outside the app — specifically from the Android **lock screen and
notification shade** — without opening the app.

**Input**: User description: "Let me control the TV from my phone's lock screen / notification
shade without opening the app — at least volume, mute, and play/pause — like the media controls a
music app shows."

## Why this feature (context)

In `002`, every control requires the app to be foregrounded. The most frequent real-world moment
for a TV remote is "phone is on the table, screen locked, I want to nudge the volume or pause" —
which today means unlock, find the app, open it, wait for the socket, tap. Android already has a
first-class surface for exactly this: a **MediaSession-backed media notification**, which the
system renders as transport controls on the lock screen and in the notification shade (and in the
Android 13+ media-controls area of Quick Settings). This feature exposes a controls-only media
notification whose buttons call the **existing SSAP commands** (`volumeUp`/`volumeDown`,
`setMute`, `playPause`) over the **existing live connection** owned by `TvConnectionManager`.

This is deliberately a **controls-only** slice. Rich "now playing" metadata (current app, title,
artwork, real play/pause state, scrubbing) depends on reliable now-playing information from the TV
that webOS does not give us cheaply, and is scoped as a **separate feature `004` (now-playing
metadata)**. `003` shows a minimal, honest notification — a TV name, a generic state line, and
buttons — and `004` will later enrich the same notification with real metadata. Keeping them apart
lets `003` ship a usable lock-screen remote without blocking on the harder metadata problem.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Control volume and mute from the lock screen (Priority: P1)

While my phone is locked, a media notification for my TV is on the lock screen with volume-down,
mute, and volume-up buttons. I tap them and the TV responds — without unlocking or opening the app.

**Why this priority**: Volume/mute is the highest-frequency reason to grab the remote and the whole
point of a lock-screen control. Without it, the feature has no value.

**Independent Test**: With a paired, connected TV and the app having been opened at least once,
lock the phone, wake the lock screen, and from the TV media notification tap volume-up/down and
mute; observe the TV respond within a moment and the notification stay present.

**Acceptance Scenarios**:

1. **Given** the app is connected to the TV and the phone is locked, **When** I wake the screen,
   **Then** a media notification identifying my TV is visible on the lock screen with volume-down,
   mute, and volume-up controls.
2. **Given** that notification, **When** I tap volume-up, **Then** the TV volume increases (calls
   the existing `volumeUp` SSAP command) and the notification remains.
3. **Given** that notification, **When** I tap mute, **Then** the TV mutes/unmutes (calls the
   existing `setMute` command) and the mute button reflects the new state.
4. **Given** the notification shade is pulled down while the phone is unlocked, **When** I look at
   the media area, **Then** the same TV controls are present there too.

---

### User Story 2 - Play/pause from the lock screen (Priority: P1)

The TV media notification also has a play/pause button so I can pause whatever is on screen without
reaching for the physical remote or opening the app.

**Why this priority**: Play/pause is the second everyday action and is the canonical centre button
of a media notification; pairing it with volume completes the "real media control" expectation.

**Independent Test**: With a connected TV playing something, lock the phone and tap play/pause on
the TV media notification; observe playback toggle on the TV.

**Acceptance Scenarios**:

1. **Given** the connected TV and a locked phone, **When** I tap play/pause on the notification,
   **Then** playback toggles on the TV (calls the existing `playPause` command).
2. **Given** the app's own play/pause tracking, **When** the notification's play/pause is used,
   **Then** the in-app and notification states do not contradict each other (single source of
   truth for the toggle).
3. **Given** webOS gives no reliable play-state query, **When** the true play state is unknown,
   **Then** the button uses the same best-effort toggle behaviour as the in-app control rather than
   pretending to know the real state. *(Honest play state is part of `004`.)*

---

### User Story 3 - Tap the notification to open the app (Priority: P2)

Tapping the body of the TV media notification opens the full remote app, so the notification is a
shortcut into everything `002` offers, not a dead end.

**Why this priority**: A natural, expected affordance; low cost. Not P1 because the controls
themselves (US1/US2) already deliver the core value.

**Independent Test**: Tap the notification body (not a button); the app opens to the remote screen.

**Acceptance Scenarios**:

1. **Given** the TV media notification, **When** I tap its body, **Then** the remote app opens to
   its main screen.
2. **Given** the phone is locked, **When** I tap the notification body, **Then** the system handles
   unlock as usual before showing the app (no control bypasses the lock for app launch).

---

### User Story 4 - The notification reflects connection state (Priority: P2)

The notification only offers working controls when the TV is actually controllable. When the TV
is disconnected, off, or off-network, the controls do not silently do nothing — they are
disabled/greyed, the notification shows a clear "not connected" line, or it goes away.

**Why this priority**: Constitution Principle IV (Resilient Connectivity) — a control that lies
about its state is worse than no control. Important, but the happy path (US1/US2) is the MVP.

**Independent Test**: With the notification showing, turn the TV off (or take the phone off
Wi-Fi); confirm the controls no longer appear to succeed — they grey out, show a disconnected
state, or the notification is removed — and recover when the TV is reachable again.

**Acceptance Scenarios**:

1. **Given** the TV is connected and the notification is showing, **When** the connection drops
   (TV off, socket closed, or phone off-network per `002`'s `OffNetwork` state), **Then** the
   notification reflects that it cannot control the TV rather than appearing to send commands into
   the void.
2. **Given** a disconnected state, **When** the app/`TvConnectionManager` auto-reconnects (FR-010
   of `002`), **Then** the notification's controls become active again.
3. **Given** a control is tapped during a brief reconnect window, **When** the command cannot be
   sent, **Then** it is rejected the same way `TvConnectionManager.request` already rejects when
   not connected — never silently dropped.

---

### User Story 5 - The notification appears while connected and goes away when it shouldn't (Priority: P3)

The control notification is present whenever the app is connected (including while backgrounded),
and is dismissed when there is no active TV to control or the user no longer wants it — so I don't
get a permanent, meaningless notification.

**Why this priority**: Lifecycle hygiene and respecting the user's notification space. Refinement
over the core controls, hence P3.

**Independent Test**: Connect to a TV, background the app, confirm the notification persists; then
disconnect/forget the TV and confirm the notification (and its foreground service) is removed.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I background the app, **Then** the control notification stays
   present so I can use it from the lock screen.
2. **Given** there is no active/paired TV (fresh install, or the TV was forgotten), **When** I look
   at the shade, **Then** there is no orphan TV control notification.
3. **Given** the foreground service backing the notification, **When** the controls are no longer
   relevant (deliberate disconnect, or the app is fully stopped by the user), **Then** the service
   stops and the notification is removed rather than lingering.

---

### Edge Cases

- **Notification permission denied (Android 13+/`minSdk 31`)**: posting notifications needs the
  runtime `POST_NOTIFICATIONS` permission from Android 13 on; if the user denies it, the lock-screen
  controls simply cannot appear. The app MUST explain (in-app) that lock-screen controls need
  notification permission and offer a path to settings, and the rest of `002` MUST keep working
  unaffected. *(A foreground service can technically start without the permission, but its
  notification will not be shown — so for this feature the permission is effectively required.)*
- **App killed by the system / swiped away**: when the backing foreground service is torn down, the
  notification goes with it. On next app open / reconnect the notification is re-established. The app
  MUST NOT present a stale notification whose buttons no longer work.
- **TV powered off from the notification's perspective**: there is no power button in this slice
  (Wake-on-LAN / power-on is out of scope, consistent with `002`). A disconnected TV greys the
  controls (US4) rather than offering an action that cannot work.
- **Lock-screen privacy setting hides notification content**: if the user's lock screen is set to
  hide sensitive notification content, the controls may be reduced or hidden by the system; this is
  the OS's call, not a bug. The notification SHOULD carry no sensitive content (it is just a TV name
  and transport buttons).
- **Multiple/secondary media sessions**: if a music/video app is also showing media controls, the
  system may show ours alongside or behind theirs in the media carousel. The TV controls MUST be
  clearly attributed to this app/TV so the user does not confuse them with another player.
- **Hardware volume rocker while locked**: `002` notes the rocker drives the TV only while the app
  is *foregrounded*. Whether an active media session lets the rocker reach the TV while locked is a
  **verification item** (see FR-013); the lock-screen *buttons* are the guaranteed path in this
  slice.
- **Doze / background restrictions**: a long-lived foreground service is subject to OEM battery
  management; the notification may be dropped on aggressive devices. The app SHOULD recover the
  notification on the next foreground/reconnect and SHOULD NOT fight the OS.
- **Stale play/pause label**: because webOS gives no reliable play-state, the play/pause button can
  show the wrong glyph after the TV is controlled by another remote. This is accepted for `003` and
  addressed by `004`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST expose a media-style notification for the active TV that the system
  renders on the **lock screen** and in the **notification shade** (and the Android 13+ media
  controls area), backed by an Android **media session** so the OS treats it as media controls.
- **FR-002**: The notification MUST provide working **volume-down, mute, and volume-up** controls
  that invoke the existing SSAP commands (`Commands.volumeDown`, `Commands.setMute`,
  `Commands.volumeUp`) over the existing live connection — no new TV-side capability.
- **FR-003**: The notification MUST provide a **play/pause** control that invokes the existing
  `Commands.playPause`, sharing the same single play/pause source of truth as the in-app control so
  the two never contradict each other.
- **FR-004**: Tapping the notification body MUST open the remote app to its main screen (US3),
  going through the normal system unlock when the phone is locked.
- **FR-005**: The notification's controls MUST reflect connection state: when the TV is not
  controllable (disconnected / off / `OffNetwork`), the controls MUST be disabled/greyed or the
  notification removed, and MUST NOT appear to succeed when no command can be sent.
- **FR-006**: When a control is tapped but the TV is not connected, the action MUST be rejected
  consistently with `TvConnectionManager.request` (visible failure, never a silent drop), and
  SHOULD surface feedback when the app is open.
- **FR-007**: The notification MUST be present while the app holds a live connection to the active
  TV, **including while the app is backgrounded**, and MUST be removed when there is no active TV
  or the user deliberately disconnects/stops the app (US5).
- **FR-008**: The notification MUST be backed by a **foreground service** of the appropriate media
  type so the controls can persist while the app is not foregrounded, with the manifest declaring
  the required foreground-service type and permissions for `minSdk 31` through the current target.
- **FR-009**: On Android 13+ (i.e. across the whole `minSdk 31` range, since 13 is API 33), the app
  MUST request the runtime **`POST_NOTIFICATIONS`** permission and, if denied, explain in-app that
  lock-screen controls require it and offer a recovery path — while the rest of `002` keeps working.
- **FR-010**: The feature MUST reuse `002`'s connection ownership: the notification/service MUST act
  through the **same `TvConnectionManager`/`Commands`** instance and the single socket, and MUST NOT
  open a second connection to the TV.
- **FR-011**: The feature MUST NOT introduce any backend, relay, or cloud path; all control still
  goes phone→TV directly over the LAN (Constitution Principle III). The notification MUST NOT carry
  the pairing credential or any usage data off-device.
- **FR-012**: For this slice the notification's metadata MUST be **minimal and honest** — the TV's
  friendly name and a generic state line are sufficient; the app MUST NOT fabricate now-playing
  titles or artwork. Rich now-playing metadata is explicitly deferred to feature **`004`**, which
  will enrich this same notification.
- **FR-013** *(verification item)*: The app SHOULD determine whether an active media session lets
  the phone's **hardware volume rocker** reach the TV while the phone is **locked/backgrounded**
  (beyond the foreground-only behaviour `002` documents). If feasible and well-behaved, it MAY route
  the rocker to `volumeUp`/`volumeDown`; if it interferes with the phone's own media/ring volume, it
  MUST NOT, and the on-notification buttons remain the guaranteed control path.
- **FR-014**: The app SHOULD provide an in-app way to turn the lock-screen controls **off**, for
  users who do not want a persistent notification, without affecting the rest of the remote.

### Key Entities *(include if feature involves data)*

- **TV Media Notification**: the system-rendered media notification for the active TV. Attributes:
  the TV's friendly name (from the active `TvConnection`), a generic state line, an ordered set of
  transport actions (vol-down, play/pause, vol-up, mute), a content tap target (open app), and a
  link to the media session. Not persisted; derived live from connection state. Metadata is minimal
  in `003`; enriched in `004`.
- **Media Session**: the OS-facing handle that marks this notification as media controls and routes
  the system's transport/volume intents to the app. One session for the one active TV; bound to the
  same connection `002` already owns.
- **Control Service (foreground)**: the foreground service that keeps the media session and
  notification alive while the app is backgrounded. Lifecycle is tied to "there is an active TV to
  control"; started on connect, stopped on disconnect/forget/stop.
- **(Reused) Connection State**: `002`'s connection state (needs-pairing / connecting / connected /
  disconnected / off-network) — the notification's enabled/greyed/removed presentation derives from
  it; no new state is introduced.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With a connected TV and the phone locked, the user can change the TV volume from the
  lock screen in a single tap, with no unlock and no app launch.
- **SC-002**: A volume/mute/play-pause tap on the notification is reflected on the TV within ~1
  second on a typical home network — i.e. no slower than the equivalent in-app control in `002`.
- **SC-003**: In 100% of cases where the TV is off or off-network, the notification controls do not
  appear to succeed: they are greyed/disabled or the notification is absent.
- **SC-004**: While the app is backgrounded but connected, the TV control notification remains
  present and functional on the lock screen and in the shade.
- **SC-005**: When the user denies notification permission, the lock-screen controls are absent and
  the user is told why, while every other `002` feature still works.
- **SC-006**: When the active TV is disconnected or forgotten, the notification and its foreground
  service are removed within a few seconds — no orphan notification remains.
- **SC-007**: No second socket to the TV is opened for the notification — the feature operates
  entirely through the connection `002` already owns.

## Assumptions

- **Builds on `002`**: pairing, the SSAP client, `TvConnectionManager`, and `Commands` already
  exist and work; this feature is a new surface over them, not a re-implementation.
- **Controls-only**: `003` ships transport/volume controls with minimal metadata. Real now-playing
  metadata (current app, title, artwork, accurate play state, scrub) is **feature `004`**, layered
  onto the same notification.
- **Same-LAN, no backend**: unchanged from `002` — control is phone→TV over the local network;
  away-from-home is out of scope.
- **Persistent connection while controls are wanted**: lock-screen controls imply the app keeps a
  live socket via a foreground service while backgrounded; this is a deliberate battery/UX tradeoff
  (see Open Questions). The controls are only meaningful when connected.
- **No power-on**: there is no power/Wake-on-LAN button in this slice; a TV that is off shows greyed
  controls (consistent with `002`).
- **Android only, `minSdk 31`, current target**: the `POST_NOTIFICATIONS` runtime permission (API
  33+) and mandatory foreground-service types (Android 14+) both fall inside this range and MUST be
  handled.
- **English-only UI** for v1, consistent with `002`.

## Clarifications

### Session 2026-06-20 (initial draft)

- **Scope split**: `003` is controls-only; **rich now-playing metadata is a separate spec `004`**
  that enriches the same notification. This keeps `003` shippable without solving webOS now-playing.
- **Buttons in scope**: volume-down, volume-up, mute, play/pause, and tap-to-open. No power button
  (Wake-on-LAN is out of scope, as in `002`).
- **Reuse, don't duplicate**: the notification calls the existing `Commands` over the existing
  `TvConnectionManager` socket; it MUST NOT open a second connection.

### Research notes (modern Android approach — to confirm at /plan)

These inform planning; specifics are an implementation concern, but the spec is written aware of
them so its requirements are realistic.

- **MediaSession API choice**: Google now recommends **Jetpack Media3** (`androidx.media3.session`)
  over the deprecated `MediaSessionCompat`/`android.support.v4.media`; the `media` compat artifacts
  are no longer updated. *However*, Media3's `MediaSession`/`MediaSessionService` is built around a
  `Player` (ExoPlayer-style **local** playback) and auto-publishes a MediaStyle notification from
  player state. **This app has no local Player** — it is a remote that forwards SSAP commands. So
  the plan must choose between: (a) a small **custom `Player`/session adapter** that maps transport
  callbacks to SSAP commands and synthesises a "playing" state, or (b) a **hand-built MediaStyle
  notification + a thin media session** posted by our own foreground service. This "no local player"
  mismatch is the single biggest design decision and an explicit **`/plan` research item**.
  *(Sources below.)*
- **What shows on the lock screen**: from **Android 13**, the media-control action buttons are
  derived from the session's `PlaybackState` actions (not raw notification actions); attaching the
  media-session token lets System UI treat it as media and render it on the lock screen / shade /
  media carousel. The expanded notification shows up to 5 actions; compact/standard view promotes a
  smaller subset — enough for our vol-down / play-pause / vol-up (+ mute) layout.
- **`POST_NOTIFICATIONS` (Android 13+ / API 33)**: a **runtime** permission since Android 13; within
  our `minSdk 31` range it must be requested at runtime, and if denied the notification is not shown
  (so it is effectively required for this feature) — even though a foreground service can start
  without it.
- **Foreground service type (Android 14+)**: declaring a `foregroundServiceType` is **mandatory** on
  Android 14+ or the app crashes (`MissingForegroundServiceTypeException`). The natural type is
  **`mediaPlayback`**, which also needs `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  permissions. **Policy caveat / verification item**: `mediaPlayback` is documented for *continuing
  audio/video playback*; this app plays nothing locally, it remotes a TV. The plan must confirm
  `mediaPlayback` is the appropriate type for a controls-only remote (it is the type the media-notif
  surface expects, and the app does represent an active media-control session for the TV), and note
  that sideload distribution (per `002`) sidesteps Play Store policy review but the type must still
  be technically correct.

Sources:
- Media3 / MediaSession recommendation & migration: <https://developer.android.com/media/media3/exoplayer/migration-guide>, <https://developer.android.com/media/implement/surfaces/mobile>
- MediaSession control of playback (Player-centric model): <https://developer.android.com/media/media3/session/control-playback>, <https://developer.android.com/media/media3/session/background-playback>
- MediaStyle / lock-screen rendering & Android 13 PlaybackState actions: <https://developer.android.com/media/implement/surfaces/mobile>, <https://developer.android.com/reference/androidx/media3/session/MediaStyleNotificationHelper.MediaStyle>
- `POST_NOTIFICATIONS` runtime permission (Android 13+): <https://developer.android.com/develop/ui/views/notifications/notification-permission>
- Foreground service types required (Android 14+) & `mediaPlayback`: <https://developer.android.com/about/versions/14/changes/fgs-types-required>, <https://developer.android.com/develop/background-work/services/fgs/service-types>
