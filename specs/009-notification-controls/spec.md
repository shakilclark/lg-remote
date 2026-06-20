# Feature Specification: Notification Controls (lock screen + shade media controls)

**Feature Branch**: `009-notification-controls`

**Created**: 2026-06-20

**Status**: Draft

**Supersedes**: `003-lockscreen-controls`. On Android, the **lock-screen media controls and the
notification-shade media controls are one and the same surface** — a single MediaSession-backed
MediaStyle notification that the system renders in the shade, on the lock screen, and in the
Android 13+ media-controls carousel. `003` scoped that surface as "controls-only" and deferred
metadata to `004`. Now that `004`'s shared `NowPlaying` snapshot is built, this spec consolidates
the whole notification surface (controls **and** the now-playing display) into one feature and
**absorbs `003`**. Recommendation: fold `003` into `009` — keep `003` on file as the historical
controls-only scope, but `009` is the spec that is implemented. There is no separate "lockscreen"
deliverable to build.

**Builds on**:
- `002-native-android-remote` — the SSAP client, `TvConnectionManager`, `Commands`, connection state.
- `004-now-playing` — the shared `nowPlaying: StateFlow<NowPlaying?>` snapshot on `RemoteViewModel`
  (app id + resolved name + icon + `PlayState`). The notification renders **this same snapshot** so
  the in-app now-playing bar and the notification never diverge.
- `008-seamless-reconnect` — **Tier 2** of that spec is the foreground service that keeps the SSAP
  socket alive while the phone is locked/backgrounded. That service is the prerequisite for this
  notification to be controllable while locked, and is built **with** this feature (per `008` OQ4).

**Input**: User description: "Give me a media notification — on the lock screen and in the
notification shade — that shows what's playing on the TV and lets me control it (play/pause,
volume, etc.) without opening the app."

## Why this feature (context)

The most frequent real-world moment for a TV remote is "phone on the table, screen locked, I want
to pause or nudge the volume". In `002` every control needs the app foregrounded: unlock, find the
app, open it, wait for the socket, tap. Android already has a first-class surface for exactly this —
a **MediaSession-backed MediaStyle notification** that System UI renders as transport controls on
the lock screen, in the shade, and (Android 13+) in the Quick Settings media carousel.

This feature exposes one such notification for the active TV. It shows **what `004` already knows is
playing** (foreground app name + icon, best-effort play-state) and offers transport + volume buttons
that call the **existing `Commands`** over the **existing single connection**, with **no app launch**.
Because the controls must work while the phone is locked, the connection has to stay alive while
backgrounded — which is the `008` Tier 2 foreground service. The persistent notification plus the
battery cost of holding the socket are the deliberate trade-off for "control the TV without opening
the app".

### One surface, not two (why this supersedes `003`)

`003` was written as "controls-only now, metadata later in `004`", on the assumption the two could
ship independently. In practice the **lock screen and the shade are the same MediaStyle notification**
— you cannot ship a lock-screen control without also shipping the shade control, and vice versa, and
the metadata (`004`) is just fields on that same notification. With `004` now built and exposing the
canonical `NowPlaying` snapshot, there is no reason to keep them split. `009` is the single spec for
that surface: it renders `004`'s snapshot and drives `004`/`002`'s commands.

## Research findings *(modern Android approach — informs requirements; specifics confirmed at /plan)*

### MediaSession API choice: Jetpack Media3, with a custom Player adapter

Google now recommends **Jetpack Media3** (`androidx.media3.session`) over the legacy
`MediaSessionCompat` / `android.support.v4.media` (the `media` compat artifacts are no longer
updated). Media3's `MediaSession` + `MediaSessionService` will **auto-publish a MediaStyle
notification from player state**, render it on the lock screen/shade/carousel, and route transport
intents (including hardware buttons and the media carousel) back to the app. [1][2][3]

The catch is that Media3 is built around a `Player` (ExoPlayer-style **local** playback), and **this
app has no local player** — it forwards SSAP commands to a TV. The modern, idiomatic way to bridge
that gap is a **custom `Player` adapter**: Media3 provides **`SimpleBasePlayer`** specifically to
"minimise the number of methods you need to implement to integrate with a custom player" — it
enforces valid player state, informs listeners of state changes, and ignores any `Command` you don't
declare available. Media3 "works with any `Player` implementation, including ExoPlayer, `CastPlayer`,
or a custom implementation" — a remote-to-TV adapter is the same shape as the `CastPlayer` case
(commands go to a remote device, not a local codec). [4][5][6]

So the plan's central design decision is between:
- **(a) `SimpleBasePlayer` adapter (recommended)** — a small custom `Player` whose available commands
  are `play/pause/stop/rewind/fastForward` + volume, each forwarding to the matching `Commands`
  method, and whose reported `PlaybackState`/`isPlaying` is **synthesised from `004`'s `NowPlaying`
  snapshot** (`PlayState.Playing/Paused` → playing/paused; `Unknown` → best-effort). Media3 then
  renders and updates the notification automatically and routes all transport intents for free.
- **(b) hand-built MediaStyle notification + thin `MediaSessionCompat`** posted by our own foreground
  service. More manual control but more boilerplate, on a deprecated API, and we'd re-implement what
  Media3 gives for free.

This "no local player → custom Player adapter" mapping is the single biggest design item and an
explicit **`/plan` research/decision item**; (a) is the recommended direction.

### What shows on the lock screen / what actions are available

From **Android 13**, System UI derives the media-control buttons from the **session's
`PlaybackState` actions** (not raw notification actions), and shows **up to five** action slots; the
**compact view shows the first three** slots. Attaching the media-session token is what makes System
UI treat the notification as media and render it on the lock screen, shade, and carousel. Five
expanded slots comfortably fit our **vol-down / play-pause / vol-up + mute** layout (plus rewind /
fast-forward in the expanded view from `004`'s transport set); the compact three would be the core
vol-down / play-pause / vol-up. [3][7]

### `POST_NOTIFICATIONS` runtime permission (Android 13+ / API 33)

A **runtime** permission since Android 13. Our `minSdk 31` range straddles 13, so it must be
requested at runtime; if denied, the notification is **not shown** — so for this feature the
permission is effectively required (a foreground service can technically start without it, but its
notification won't appear). The rest of `002` must keep working if it's denied. [8]

### Foreground service type (`mediaPlayback`) — required on Android 14+

Declaring a `foregroundServiceType` is **mandatory on Android 14+** or the app crashes with
`MissingForegroundServiceTypeException` at `startForeground()`. The natural type is **`mediaPlayback`**,
which also requires the `FOREGROUND_SERVICE` **and** `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions
in the manifest, plus `android:foregroundServiceType="mediaPlayback"` on the service. `mediaPlayback`
is one of the types permitted to **run indefinitely** while showing a persistent notification — which
is exactly the "stay connected while locked" lifetime `008` Tier 2 needs. [9][10][11]

**Policy caveat / verification item**: `mediaPlayback` is documented for *continuing audio/video
playback*; this app plays nothing locally — it remotes a TV. The plan must confirm `mediaPlayback` is
the technically-correct type for a controls-only TV remote (it is the type the media-notification
surface expects, and the app *does* represent an active media-control session for the TV). Sideload
distribution (`002`) sidesteps Play Store policy review, but the declared type must still be correct.

### Sources

1. Media controls / MediaStyle surface (mobile): <https://developer.android.com/media/implement/surfaces/mobile>
2. Media3 MediaSession auto-notification & migration: <https://developer.android.com/media/media3/session/control-playback>, <https://developer.android.com/media/media3/session/background-playback>
3. Android 13 media controls derive from `PlaybackState` (up to 5 actions, compact = first 3): <https://developer.android.com/about/versions/13/behavior-changes-13>, <https://source.android.com/docs/core/display/media-control>
4. `SimpleBasePlayer` (minimal custom Player): <https://developer.android.com/media/media3/session/player>, <https://android-developers.googleblog.com/2025/01/media3-150-whats-new.html>
5. Media3 works with any `Player` (ExoPlayer / CastPlayer / custom): <https://developer.android.com/media/media3/session/control-playback>
6. Custom Player with Media3 (`SimpleBasePlayer`) discussion: <https://github.com/androidx/media/issues/2166>
7. MediaStyle / `setShowActionsInCompactView` rendering: <https://developer.android.com/media/implement/surfaces/mobile>
8. `POST_NOTIFICATIONS` runtime permission (Android 13+): <https://developer.android.com/develop/ui/views/notifications/notification-permission>
9. Foreground service types required (Android 14+): <https://developer.android.com/about/versions/14/changes/fgs-types-required>
10. Foreground service types & `mediaPlayback` permissions: <https://developer.android.com/develop/background-work/services/fgs/service-types>
11. Background playback with `MediaSessionService`: <https://developer.android.com/media/media3/session/background-playback>

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See what's playing on the TV in the notification (Priority: P1)

When the TV is connected and something is on screen, a media notification for my TV appears on the
lock screen and in the shade, showing the **app that's playing** (name + icon, e.g. "Netflix",
"YouTube", "HDMI 1") and, where the TV reports it, whether it's playing or paused — the same thing
the in-app now-playing bar shows.

**Why this priority**: The notification's whole value is "what's on + control it" at a glance; the
"what's on" comes for free from `004`'s snapshot and is what makes the controls meaningful.

**Independent Test**: With a paired, connected TV, open Netflix on the TV, lock the phone, wake the
lock screen → the TV media notification shows "Netflix" + its icon; switch to YouTube on the TV →
the notification updates to YouTube without opening the app.

**Acceptance Scenarios**:

1. **Given** a connected TV with an app in the foreground, **When** I view the lock screen / shade,
   **Then** the TV media notification shows that app's display name and icon (from `004`'s
   `NowPlaying`).
2. **Given** the notification is showing, **When** the foreground app changes on the TV, **Then** the
   notification updates to the new app within a moment, with no app launch.
3. **Given** an app that reports play-state, **When** playback is paused/resumed on the TV (by any
   remote), **Then** the notification's play/pause glyph reflects the matching state.
4. **Given** an app or source that does **not** report play-state (e.g. Netflix), **When** I view the
   notification, **Then** it shows app name + icon and a best-effort play/pause control rather than
   claiming a state it doesn't know (consistent with `004`).

---

### User Story 2 - Control volume and mute from the notification (Priority: P1)

While my phone is locked (or from the shade), the TV media notification has volume-down, mute, and
volume-up buttons. I tap them and the TV responds — without unlocking or opening the app.

**Why this priority**: Volume/mute is the highest-frequency reason to grab the remote and the core
justification for a lock-screen control. Without it the feature has no value.

**Independent Test**: With a paired, connected TV, lock the phone, from the TV media notification tap
volume-up/down and mute; the TV responds within a moment and the notification stays present.

**Acceptance Scenarios**:

1. **Given** the connected TV and a locked phone, **When** I tap volume-up, **Then** the TV volume
   increases (calls `Commands.volumeUp`) and the notification remains.
2. **Given** the notification, **When** I tap volume-down, **Then** the TV volume decreases
   (`Commands.volumeDown`).
3. **Given** the notification, **When** I tap mute, **Then** the TV mutes/unmutes (`Commands.setMute`)
   and the control reflects the new mute state from the live volume subscription.
4. **Given** the shade is pulled down while unlocked, **When** I look at the media area, **Then** the
   same TV controls are present there too (same notification, not a second one).

---

### User Story 3 - Transport controls from the notification (Priority: P1)

The TV media notification has a play/pause button (and, in the expanded view, the rest of `004`'s
transport set the TV supports) so I can pause/resume whatever is on screen without the physical
remote or opening the app.

**Why this priority**: Play/pause is the canonical centre button of a media notification; pairing it
with volume completes the "real media control" expectation.

**Independent Test**: With a connected TV playing something, lock the phone, tap play/pause on the
notification → playback toggles on the TV; expand the notification and confirm rewind/fast-forward
where shown.

**Acceptance Scenarios**:

1. **Given** the connected TV and a locked phone, **When** I tap play/pause, **Then** playback
   toggles on the TV via the **same state-driven play/pause as the in-app control**
   (`RemoteViewModel.playPause()`: pause when `004` reports Playing, play when Paused, best-effort
   toggle when Unknown).
2. **Given** the app's in-app now-playing bar and the notification, **When** play/pause is used from
   either, **Then** the two never contradict each other — both derive from the single `NowPlaying`
   snapshot.
3. **Given** the expanded notification, **When** the TV's focused app supports it, **Then**
   rewind/fast-forward (and stop) are available and call the matching `Commands`
   (`rewind`/`fastForward`/`stop`); next/previous and a scrubber are **never** shown (not supported
   over SSAP, per `004`).

---

### User Story 4 - Control the TV while the app is locked/backgrounded (Priority: P1)

The notification's controls work even though the app isn't open, because a foreground service keeps
the SSAP connection alive while the phone is locked/backgrounded — so a tap on the notification
reaches the TV with no app launch and no reconnect wait.

**Why this priority**: This is the difference between a real lock-screen remote and a notification
that only works when the app happens to be foregrounded. It is the mechanism the whole feature
depends on (and the `008` Tier 2 dependency).

**Independent Test**: With a connected TV, background the app, lock the phone, wait ~60s, then tap
volume/play-pause on the notification → the TV responds immediately, with no app window appearing and
no multi-second reconnect.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I background the app, **Then** the foreground service keeps the
   socket alive and the notification stays present and functional.
2. **Given** the app is backgrounded/locked, **When** I tap a control, **Then** the command is sent
   over the **already-open** connection (no per-tap reconnect), reusing the single `TvConnectionManager`.
3. **Given** the foreground service is running, **When** the connection is alive, **Then** there is
   **nothing to reconnect** on return to the app (this is `008` Tier 2: the socket never dropped).

---

### User Story 5 - Tap the notification to open the app (Priority: P2)

Tapping the body of the TV media notification opens the full remote app, so the notification is a
shortcut into everything `002`/`004` offer, not a dead end.

**Why this priority**: A natural, expected affordance at low cost. Not P1 because the controls
themselves (US1–US4) already deliver the core value.

**Independent Test**: Tap the notification body (not a button) → the app opens to the remote screen;
if the phone is locked, the system handles unlock first.

**Acceptance Scenarios**:

1. **Given** the TV media notification, **When** I tap its body, **Then** the remote app opens to its
   main screen.
2. **Given** the phone is locked, **When** I tap the notification body, **Then** the system handles
   unlock as usual before showing the app (no control bypasses the lock for app launch; the transport
   buttons themselves work without unlock).

---

### User Story 6 - The notification reflects connection state, honestly (Priority: P2)

The notification only offers working controls when the TV is actually controllable. When the TV is
disconnected, off, or off-network, the controls don't silently do nothing — they're disabled, show a
clear "not connected" line, or the notification goes away; and it recovers when the TV is reachable.

**Why this priority**: Constitution Principle IV (Resilient Connectivity) — a control that lies about
its state is worse than no control. Important, but the happy path (US1–US4) is the MVP.

**Independent Test**: With the notification showing, turn the TV off (or take the phone off Wi-Fi) →
the controls no longer appear to succeed (greyed / "not connected" / removed) and recover when the TV
is reachable again.

**Acceptance Scenarios**:

1. **Given** the TV is connected and the notification is showing, **When** the connection drops (TV
   off, socket closed, or `OffNetwork` per `002`), **Then** the notification reflects it cannot
   control the TV rather than appearing to send commands into the void, and `004`'s `NowPlaying` is
   cleared/marked stale (no frozen "now playing").
2. **Given** a disconnected state, **When** the connection is restored (`008` reconnect / `002`
   FR-010), **Then** the controls become active again and the now-playing display refreshes.
3. **Given** a control is tapped during a brief reconnect window, **When** the command can't be sent,
   **Then** it's rejected consistently with `TvConnectionManager.request` — never silently dropped.

---

### User Story 7 - Notification lifecycle hygiene + opt-out (Priority: P3)

The notification is present whenever the app holds a live connection (including backgrounded) and is
removed when there's no active TV or the user no longer wants it — so I never get a permanent,
meaningless notification, and I can turn the feature off if I don't want a persistent notification.

**Why this priority**: Lifecycle hygiene and respecting the user's notification space and battery.
Refinement over the core controls, hence P3.

**Independent Test**: Connect to a TV, background the app → notification persists; disconnect/forget
the TV → notification + foreground service are removed; toggle the feature off in-app → no persistent
notification and the rest of the remote still works.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I background the app, **Then** the control notification stays
   present.
2. **Given** there's no active/paired TV (fresh install, or the TV was forgotten), **When** I look at
   the shade, **Then** there's no orphan TV control notification.
3. **Given** the foreground service backing the notification, **When** the controls are no longer
   relevant (deliberate disconnect, forget, or the user stops the app), **Then** the service stops and
   the notification is removed rather than lingering.
4. **Given** a user who doesn't want a persistent notification, **When** they turn the lock-screen
   controls off in-app, **Then** the notification and its foreground service do not run, and every
   other `002`/`004` feature still works.

---

### Edge Cases

- **Notification permission denied (Android 13+ / within `minSdk 31`)**: posting needs the runtime
  `POST_NOTIFICATIONS` permission; if denied, the controls can't appear. The app MUST explain in-app
  that the notification controls need notification permission and offer a path to settings, while the
  rest of `002`/`004` keeps working unaffected. *(A foreground service can start without it, but its
  notification won't show — so the permission is effectively required here.)*
- **App killed / swiped away**: when the backing foreground service is torn down, the notification
  goes with it. On next app open / reconnect it's re-established. The app MUST NOT present a stale
  notification whose buttons no longer work.
- **OEM battery managers / Doze kill the service**: a long-lived foreground service is subject to
  aggressive OEM battery management; the socket-keeping service may be killed on some devices. The app
  SHOULD recover on next foreground/reconnect (falling back to `008` Tier 1 instant-resume) and SHOULD
  NOT fight the OS. This is the known cost of always-connected.
- **TV powered off**: there is no power button in this slice (Wake-on-LAN is out of scope, as in
  `002`/`003`). A disconnected TV greys the controls (US6) rather than offering an action that can't
  work.
- **Lock-screen privacy hides notification content**: if the user's lock screen hides sensitive
  notification content, the controls may be reduced/hidden by the system; that's the OS's call. The
  notification carries no sensitive content (a TV/app name + transport buttons).
- **Multiple media sessions** (a music/video app also showing controls): System UI may show ours
  alongside/behind theirs in the media carousel. The TV controls MUST be clearly attributed to this
  app/TV (TV/app name + icon) so they aren't confused with another player.
- **Stale play/pause glyph for non-reporting apps**: because webOS gives no reliable play-state for
  apps like Netflix, the glyph can be wrong after the TV is controlled by another remote. Accepted —
  the same `004` "Unknown → best-effort" honesty applies; we don't fabricate a state.
- **Hardware volume rocker while locked**: an active media session may let the phone's volume rocker
  reach the session. Whether to route it to `volumeUp`/`volumeDown` while locked (beyond `002`'s
  foreground-only rocker capture) is a **verification item** (FR-013): do it only if it doesn't
  hijack the phone's own media/ring volume; the on-notification buttons remain the guaranteed path.
- **Rapid app switching on the TV**: `004`'s snapshot may emit bursts; the notification must settle on
  the latest state without flicker and not queue stale frames.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST expose a **single media-style notification** for the active TV that the
  system renders on the **lock screen**, in the **notification shade**, and in the **Android 13+ media
  carousel**, backed by an Android **media session** so System UI treats it as media controls. This is
  one surface — there is no separate lock-screen vs shade notification (this is why `009` supersedes
  `003`).
- **FR-002**: The notification MUST display **what `004` reports is playing** — the foreground app's
  resolved display name and icon, and best-effort play-state — sourced from the **same
  `nowPlaying: StateFlow<NowPlaying?>`** snapshot the in-app now-playing surface uses, with no second
  derivation of "what's playing".
- **FR-002a (cover view)**: The notification MUST set a **large icon / cover** — the playing app's
  icon from the `004` snapshot (the TV's launch-point icon, loaded over the TV-trust client). Since
  webOS exposes **no real media artwork** for app playback, the app icon **is** the cover; a real
  artwork field MUST be used instead only **if available** (it isn't, for webOS app playback). The
  in-app expanded now-playing already renders this as a large cover view; the notification mirrors it.
- **FR-003**: The notification MUST provide working **volume-down, mute, volume-up** controls that
  invoke the existing SSAP commands (`Commands.volumeDown`, `Commands.setMute`, `Commands.volumeUp`)
  over the existing live connection — no new TV-side capability; mute state reflects the live volume
  subscription.
- **FR-004**: The notification MUST provide a **play/pause** control that uses the **same
  state-driven logic as the in-app control** (`RemoteViewModel.playPause()` — pause when `NowPlaying`
  is Playing, play when Paused, best-effort toggle when Unknown), so the in-app bar and the
  notification never contradict each other.
- **FR-005**: The notification's expanded view MAY offer the rest of `004`'s SSAP-supported transport
  (rewind, fast-forward, stop) via the matching `Commands`; it MUST NOT offer next/previous or a
  seek/scrubber (unsupported over SSAP, per `004`). The compact view MUST show at least the core
  vol-down / play-pause / vol-up (the first three action slots on Android 13+).
- **FR-006**: Tapping the notification body MUST open the remote app to its main screen (US5), going
  through the normal system unlock when the phone is locked; transport buttons MUST work without
  unlocking.
- **FR-007**: The controls MUST reflect connection state: when the TV is not controllable
  (disconnected / off / `OffNetwork`), the controls MUST be disabled/greyed or the notification
  removed, and MUST NOT appear to succeed when no command can be sent. The play-state/now-playing
  display MUST be cleared/marked stale rather than frozen (per `004` FR-009).
- **FR-008**: When a control is tapped but the TV isn't connected, the action MUST be rejected
  consistently with `TvConnectionManager.request` (visible failure, never a silent drop), and SHOULD
  surface feedback when the app is open.
- **FR-009**: The notification MUST be backed by a **foreground service** declared with
  `foregroundServiceType="mediaPlayback"` and the `FOREGROUND_SERVICE` +
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions, so the controls and the connection persist while
  the app is **backgrounded/locked**. This service is the `008` **Tier 2** socket-keeper: it holds the
  SSAP connection open so there is nothing to reconnect on return. *(The `mediaPlayback` type's
  applicability to a controls-only TV remote is a `/plan` verification item — see Research.)*
- **FR-010**: On Android 13+ (within the `minSdk 31` range), the app MUST request the runtime
  **`POST_NOTIFICATIONS`** permission and, if denied, explain in-app that the notification controls
  require it and offer a recovery path — while the rest of `002`/`004` keeps working.
- **FR-011**: The feature MUST reuse `002`'s connection ownership: the notification/service MUST act
  through the **same `TvConnectionManager` / `Commands`** instance and the single socket, and MUST NOT
  open a second connection to the TV.
- **FR-012**: The feature MUST NOT introduce any backend, relay, or cloud path; all control stays
  phone→TV directly over the LAN (Constitution Principle III). The notification MUST NOT carry the
  pairing credential or any usage data off-device.
- **FR-013** *(verification item)*: The app SHOULD determine whether the active media session lets the
  phone's **hardware volume rocker** reach the TV while the phone is **locked/backgrounded** (beyond
  `002`'s foreground-only rocker capture). If feasible and well-behaved, it MAY route the rocker to
  `volumeUp`/`volumeDown`; if it interferes with the phone's own media/ring volume, it MUST NOT, and
  the on-notification buttons remain the guaranteed control path.
- **FR-014**: The app MUST provide an in-app way to turn the notification controls **off**, for users
  who don't want a persistent notification or the battery cost, without affecting the rest of the
  remote (US7).
- **FR-015**: The notification + foreground service MUST be present while the app holds a live
  connection to the active TV (including backgrounded), and MUST be removed when there's no active TV
  or the user deliberately disconnects/forgets/stops the app or disables the feature (US7).

### Key Entities *(include if feature involves data)*

- **TV Media Notification**: the single system-rendered media notification for the active TV.
  Attributes: the now-playing display (app name + icon + play-state, from `004`'s `NowPlaying`), an
  ordered set of transport/volume actions (vol-down, play/pause, vol-up, mute; +rewind/ff/stop
  expanded), a content tap target (open app), and a link to the media session. Derived live from
  `NowPlaying` + connection state; not persisted.
- **Media Session (Player adapter)**: the OS-facing handle marking this notification as media and
  routing transport/volume intents to the app. Recommended form: a **Media3 `MediaSession` over a
  custom `SimpleBasePlayer`** whose available commands forward to `Commands` and whose reported
  play-state is synthesised from `004`'s `NowPlaying`. One session for the one active TV, bound to the
  connection `002` already owns. (Alternative: hand-built MediaStyle + `MediaSessionCompat`.)
- **Control / Socket-keeper Service (foreground)**: the `mediaPlayback` foreground service that keeps
  the media session, the notification, **and the SSAP socket** alive while backgrounded — the `008`
  Tier 2 mechanism. Lifecycle tied to "there is an active TV to control and the feature is enabled";
  started on connect, stopped on disconnect/forget/stop/disable.
- **(Reused) `NowPlaying` snapshot** (`004`): the single source of truth for what's playing and the
  play-state; the notification renders it and never re-derives it.
- **(Reused) Connection State** (`002`): drives the enabled/greyed/removed presentation; no new state
  is introduced.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With a connected TV and the phone locked, the user can change the TV volume from the
  lock screen in a single tap, with no unlock and no app launch.
- **SC-002**: A volume / mute / play-pause / transport tap on the notification is reflected on the TV
  within ~1 second on a typical home network — no slower than the equivalent in-app control.
- **SC-003**: The notification shows the **same app + play-state** as the in-app now-playing surface
  at all times (single `NowPlaying` snapshot) — they never disagree.
- **SC-004**: While the app is backgrounded/locked but connected, the notification remains present and
  functional, and a control tap reaches the TV over the **already-open** socket (no per-tap
  reconnect).
- **SC-005**: In 100% of cases where the TV is off or off-network, the notification controls do not
  appear to succeed (greyed/disabled or absent), and no frozen "now playing" is shown.
- **SC-006**: When the user denies notification permission, the controls are absent and the user is
  told why, while every other `002`/`004` feature still works.
- **SC-007**: When the active TV is disconnected/forgotten or the feature is disabled, the
  notification and its foreground service are removed within a few seconds — no orphan notification.
- **SC-008**: No second socket to the TV is opened for the notification — it operates entirely through
  the connection `002` already owns.

## Assumptions

- **Builds on `002` + `004` + `008`**: pairing, the SSAP client, `TvConnectionManager`, `Commands`,
  and the `nowPlaying` snapshot already exist and work; the `008` Tier 2 foreground service is built
  with this feature. This is a new surface over existing pieces, not a re-implementation.
- **One surface supersedes `003`**: lock screen and shade are the same MediaStyle notification, so
  `009` absorbs `003`'s controls-only scope plus `004`'s metadata into one deliverable.
- **Persistent connection while controls are wanted**: notification controls imply the app keeps a
  live socket via the foreground service while backgrounded — a deliberate battery/UX trade-off. The
  controls are only meaningful when connected; FR-014 lets the user opt out.
- **No rich metadata / art**: only app name + icon + best-effort play-state (the `004` ceiling); no
  title/artist/album/episode/art/scrubber for app playback. Play/pause may be optimistic for apps
  (e.g. Netflix) that don't report state.
- **OEM battery managers may kill the service**: on aggressive devices the socket-keeper service may
  be killed; the app falls back to `008` Tier 1 instant-resume on return rather than fighting the OS.
- **Same-LAN, no backend**: unchanged from `002` — control is phone→TV over the local network;
  away-from-home is out of scope.
- **No power-on**: no power / Wake-on-LAN button; an off TV shows greyed controls.
- **Android only, `minSdk 31`, current target**: `POST_NOTIFICATIONS` (API 33+) and mandatory
  foreground-service types (Android 14+) both fall inside this range and MUST be handled.
- **English-only UI** for v1, consistent with `002`.

## Clarifications

### Session 2026-06-20 (consolidation)

- **Supersedes `003`**: on Android the lock-screen and shade media controls are **one** MediaStyle
  notification, so a standalone "lockscreen controls" feature can't exist apart from the shade one.
  With `004`'s snapshot built, `009` consolidates controls + now-playing display into one spec and
  **absorbs `003`**. Recommendation: fold `003` into `009` (keep `003` on file as historical scope;
  implement `009`).
- **Single source of truth**: the notification renders `004`'s `nowPlaying` snapshot and calls
  `RemoteViewModel`/`Commands` — the in-app bar and the notification can never diverge.
- **Buttons in scope**: vol-down, vol-up, mute, play/pause (compact); + rewind / fast-forward / stop
  (expanded, per `004`'s SSAP-supported set). No next/previous, no scrubber, no power.
- **Foreground service = `008` Tier 2**: the socket-keeper service is shared with `008`; building
  `009` delivers `008`'s Tier 2. (`008` OQ4 resolved: introduce the service *with* this feature.)

### Open questions (for /clarify)

- **OQ1 — MediaSession strategy**: Media3 `MediaSession` + custom `SimpleBasePlayer` adapter
  (recommended) vs a hand-built MediaStyle + `MediaSessionCompat`? Resolve at `/plan`; this is the
  single biggest design decision.
- **OQ2 — `mediaPlayback` correctness**: confirm `mediaPlayback` is the right foreground-service type
  for a controls-only TV remote (no local playback). It's the type the media-notification surface
  expects, and sideload distribution sidesteps Play Store review — but the type must be technically
  correct (Research verification item).
- **OQ3 — compact-view layout**: which three of vol-down / play-pause / vol-up / mute go in the
  compact (3-slot) view, and which actions in the expanded (up to 5)?
- **OQ4 — hardware volume rocker while locked** (FR-013): route the rocker to TV volume while locked,
  or keep it to the on-notification buttons only to avoid hijacking the phone's own volume?
- **OQ5 — feature default**: is the persistent notification + always-connected service **on by
  default** (best UX, more battery) or **opt-in** (battery-conservative)? FR-014 provides the toggle;
  the default needs a decision.
- **OQ6 — service lifetime vs `008` Tier 1**: when the OEM kills the service, do we attempt restart,
  or silently fall back to `008` Tier 1 instant-resume until the app is next foregrounded?
