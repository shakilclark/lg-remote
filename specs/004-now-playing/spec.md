# Feature Specification: Dynamic Now-Playing Screen with Media Controls

**Feature Branch**: `004-now-playing`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the native Android, direct-LAN remote). This adds an
in-app "now playing" surface that shows what is on the TV right now and offers media transport
controls, updating live. It also produces the live media state that `003` (lockscreen / notification
controls) consumes — the two features share one source of truth for "what's playing".

**Input**: User description: "A now-playing screen that shows what's currently playing on the TV
(foreground app, and any media info like title / channel / art) with transport controls
(play/pause, rewind/fast-forward/stop, next/prev where supported), updating live."

## Why this feature (context)

The `002` remote already toggles play/pause blind: it tracks the *last* action it sent because
"webOS has no reliable play-state query" (see `Commands.kt`). That means the play/pause button can
get out of sync with the TV, and the user never sees *what* is playing or whether it's actually
playing or paused. webOS does expose a live foreground-app + play-state feed over SSAP, which lets
us replace the blind toggle with a screen that reflects real TV state and offers the transport
controls the TV supports. This is also the state `003` needs for lockscreen controls, so building it
once here and feeding both surfaces avoids two diverging notions of "now playing".

A hard reality shapes this whole feature: **SSAP exposes very little media metadata** — see the
Research findings below. The design therefore degrades gracefully: it shows whatever the TV will
tell us (at minimum the foreground app name + icon + play-state) and never promises rich
title/artist/art it cannot reliably deliver.

## Research findings *(what SSAP can and can't tell us about "now playing")*

This is the research-heavy part of the spec; user stories below are scoped strictly to what is
**actually achievable** over SSAP on a real LG webOS TV.

### What IS reliably available

- **Foreground app identity** — `ssap://com.webos.applicationManager/getForegroundAppInfo`,
  **subscribable**. Returns `appId` (e.g. `netflix`, `youtube.leanback.v4`,
  `com.webos.app.hdmi1`), plus `windowId`/`processId`. We already pull `listLaunchPoints`
  (`Commands.kt`), so we can map `appId` → human title + icon for display. This is the backbone of
  "now playing": it always tells us *which app/source* is on screen. [1][6][8]
- **Play / pause / stopped state** — `ssap://com.webos.media/getForegroundAppInfo` (the **media
  server** variant, distinct from the applicationManager one above), **subscribable**. Returns a
  `foregroundAppInfo` array whose entries carry `playState` (`playing` / `paused` / etc.), `type`
  (`media`), `mediaId`, `appId`, `windowId`. This gives a *real* play-state we can reflect in the
  UI and use to fix the blind toggle — but only when an app reports media to the webOS media server
  (see limitations). [2][3]
- **Volume / mute** — `ssap://audio/getVolume` (already subscribed in `002`). Useful context to
  show alongside transport controls. [contracts/ssap-protocol.md]
- **Audio output** — `ssap://audio/getSoundOutput` (TV speaker / ARC / optical). Minor context. [9]
- **Live channel + programme (broadcast/antenna only)** — `ssap://tv/getCurrentChannel` and
  `ssap://tv/getChannelProgramInfo` return channel name/number and EPG programme info. This is the
  **only** path to a real human-readable "title" (the programme name) — and it works **only for the
  live-TV tuner input**, not for streaming apps. [4]

### What is NOT available (be blunt)

- **No rich media metadata over SSAP.** There is **no** command that returns the playing item's
  **title, artist, album, episode name, duration, elapsed position, or album/poster art** for
  streaming apps (Netflix, YouTube, Disney+, Spotify, etc.). The media-server `getForegroundAppInfo`
  gives `playState`/`mediaId` only; `mediaId` is an opaque handle, not a title. Confirmed by
  surveying the established webOS client libraries (`go-webos`, `pywebostv`, `webostv`) — **none**
  expose a metadata getter, and the Home Assistant webOS integration tracks `playState` only for the
  same reason. [2][3][5][7][9] So "show the song/episode title and album art" is **not deliverable**
  for app playback. The screen can show app name + icon + play-state, and (for live TV) channel +
  programme — nothing richer.
- **No next / previous track command.** SSAP media transport is limited to
  `play`, `pause`, `stop`, `rewind`, `fastForward` (`ssap://media.controls/*`). There is **no**
  `next`/`previous`/`skip` URI in any reviewed library. "Next/prev where supported" is therefore
  **not supported** via the documented media-controls socket. (A skip *gesture* could be faked by
  sending a pointer-socket button, but that is app-dependent and unreliable, so it is explicitly out
  of scope here.) [1][5]
- **No seek-to-position / scrubber.** No position is reported and no seek URI exists for app
  playback; a progress bar / scrubber is not possible.
- **playState is not universal.** Many apps (notably some versions of Netflix/YouTube and most
  HDMI/external sources) do **not** register with the webOS media server, so the media-server
  `getForegroundAppInfo` may return an empty `foregroundAppInfo` array or omit `playState`. When
  that happens we cannot know if it's playing or paused — the UI must degrade to "unknown" and the
  transport buttons act as best-effort fire-and-forget (same caveat as `002`'s blind toggle).
- **`media.controls` are fire-and-forget.** They return only `returnValue`; whether the focused app
  honours `play`/`pause`/`rewind`/`fastForward` is app-dependent. The screen reflects *reported*
  state, not a guarantee the command landed.

### How `001` handled media (for completeness)

`001` (PWA + `lgtv2` backend) did **not** read media metadata or play-state at all. It only sent
`ssap://media.controls/play` and `/pause` and **tracked the last action in backend state** to choose
which to send next (`contracts/tv-protocol.md`: "the backend tracks the last media state to choose
play vs pause, falling back to pause when unknown"). `002` carried that blind-toggle forward in
`Commands.kt` (`private var playing: Boolean?`). This feature is the first time the app reads real
TV play-state rather than guessing.

### How we subscribe for live updates

Use the **same `manager.subscribe(uri)` pattern already proven for volume** (`Commands.kt`
`volumeUpdates()` → `manager.subscribe("ssap://audio/getVolume")`, surfaced as a `Flow`). The
now-playing state is the merge of three subscriptions —
`com.webos.applicationManager/getForegroundAppInfo` (app identity),
`com.webos.media/getForegroundAppInfo` (play-state), and (for live TV)
`tv/getCurrentChannel` / `tv/getChannelProgramInfo` — combined into one observable `NowPlaying`
snapshot. No new transport mechanism is needed; subscriptions stop when the socket drops and resume
on reconnect (Principle IV).

### Sources

1. `com.webos.applicationManager/getForegroundAppInfo` (subscribe) — webOS OSE applicationmanager
   LS2 API: https://www.webosose.org/docs/reference/ls2-api/com-webos-service-applicationmanager/
2. `com.webos.media` LS2 API (media server): https://www.webosose.org/docs/reference/ls2-api/com-webos-media/
3. Home Assistant core #91709 "webOS TV doesn't report media playback status" — documents that
   `com.webos.media/getForegroundAppInfo` returns `playState`/`type`/`mediaId`/`appId` and nothing
   richer: https://github.com/home-assistant/core/issues/91709
4. `tv/getCurrentChannel` / `tv/getChannelProgramInfo` — go-webos commands:
   https://github.com/kaperys/go-webos/blob/master/commands.go
5. `media.controls` URIs (play/pause/stop/rewind/fastForward; no next/prev) — go-webos commands
   (link above) and pywebostv MediaControl:
   https://github.com/supersaiyanmode/PyWebOSTV/blob/master/pywebostv/controls.py
6. webOS Homebrew commands cheatsheet (getForegroundAppInfo subscribe example):
   https://www.webosbrew.org/pages/commands-cheatsheet
7. snabb/webostv app/media commands: https://github.com/snabb/webostv/blob/master/mapps.go
8. openlgtv hacking notes: https://gist.github.com/Informatic/1983f2e501444cf1cbd182e50820d6c1
9. pywebostv controls (MediaControl has no metadata getter; SourceControl = inputs; getSoundOutput):
   https://github.com/supersaiyanmode/PyWebOSTV/blob/master/pywebostv/controls.py

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See what's on the TV right now (Priority: P1)

When I open the now-playing screen, I see what is currently on the TV: the foreground app's name
and icon (e.g. "Netflix", "YouTube", "HDMI 1"), and whether it is playing or paused. As I change
apps or pause on the TV (or via another remote), the screen updates live without me refreshing.

**Why this priority**: This is the core of the feature and the one thing SSAP can deliver reliably
for every source. Even with no rich metadata, knowing *what* is on and *whether it's playing* is
the whole value; transport controls (US2) build on it.

**Independent Test**: With a paired TV, open YouTube on the TV → the screen shows "YouTube" + icon;
switch to HDMI 1 → it updates to the HDMI source; pause on the TV → the screen reflects "paused"
(where the app reports play-state).

**Acceptance Scenarios**:

1. **Given** a connected TV with an app in the foreground, **When** I open the now-playing screen,
   **Then** I see that app's display name and icon.
2. **Given** the now-playing screen is open, **When** the foreground app changes on the TV, **Then**
   the screen updates to the new app within a moment, without a manual refresh.
3. **Given** an app that reports play-state, **When** playback is paused or resumed on the TV,
   **Then** the screen shows the matching playing/paused state.
4. **Given** an app or source that does **not** report play-state, **When** I view the screen,
   **Then** it shows the app name/icon and an explicit "playback state unknown" affordance rather
   than guessing.

---

### User Story 2 - Control playback from the now-playing screen (Priority: P1)

From the now-playing screen I can drive transport with large buttons: play/pause, stop, rewind, and
fast-forward. The play/pause button reflects the *real* TV state where the TV reports it (no more
guessing), and falls back to a best-effort toggle when it doesn't.

**Why this priority**: Transport control is the point of a "now playing" surface and is the natural
home for the controls `002` exposes blindly. It directly improves the existing play/pause by binding
it to real state.

**Independent Test**: With media playing on the TV, tap pause → it pauses and the button flips to
"play"; tap rewind / fast-forward → the TV scrubs (in apps that honour it); tap stop → playback
stops.

**Acceptance Scenarios**:

1. **Given** media reported as playing, **When** I tap pause, **Then** the TV pauses and the button
   reflects the paused state.
2. **Given** media reported as paused, **When** I tap play, **Then** the TV resumes and the button
   reflects playing.
3. **Given** any focused media app, **When** I tap rewind or fast-forward, **Then** the
   corresponding `media.controls` command is sent (best-effort; honoured per app).
4. **Given** any focused media app, **When** I tap stop, **Then** the stop command is sent.
5. **Given** the TV does not report play-state, **When** I tap play/pause, **Then** the app sends a
   best-effort toggle (as `002` does today) and indicates the state is unconfirmed.
6. **Given** next/previous are not supported over SSAP, **When** I view the controls, **Then** no
   next/previous buttons are shown (they are not offered, to avoid dead controls).

---

### User Story 3 - See live-TV channel & programme (Priority: P3)

When the TV is on a broadcast/antenna input, the now-playing screen shows the channel name/number
and the current programme name — the one case where SSAP gives a real human-readable "title".

**Why this priority**: A genuine metadata win, but it applies only to live-TV viewing (not the
streaming apps most used here), so it is an enhancement on top of the app-identity core (US1).

**Independent Test**: With the TV on a live-TV channel, open the now-playing screen → it shows the
channel and the current programme; change channel → the displayed channel/programme updates.

**Acceptance Scenarios**:

1. **Given** the TV is on a live-TV channel, **When** I open the now-playing screen, **Then** it
   shows the channel name/number.
2. **Given** programme info is available for that channel, **When** I view the screen, **Then** the
   current programme name is shown.
3. **Given** the TV is **not** on live TV (a streaming app or HDMI), **When** I view the screen,
   **Then** no channel/programme block is shown (it degrades to US1's app identity).

---

### User Story 4 - Feed the lockscreen / notification controls (Priority: P2)

The same live now-playing state (app name, icon, play-state) is published so the `003` lockscreen /
notification media controls show the right app and reflect playing/paused — one source of truth, no
divergence between the in-app screen and the notification.

**Why this priority**: `003`'s lockscreen controls depend on this state existing; exposing it as a
shared, observable snapshot is what lets `003` be built without re-deriving "what's playing". It is
P2 because `003` is a separate feature — this story is the contract, not the lockscreen UI itself.

**Independent Test**: With now-playing active and `003` present, change the foreground app on the TV
→ the lockscreen/notification updates to the same app and play-state the in-app screen shows.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** the foreground app or play-state changes, **Then** the
   published now-playing snapshot updates and any consumer (the `003` notification) sees the same
   values as the in-app screen.
2. **Given** the TV becomes unreachable, **When** the connection drops, **Then** the published
   snapshot is marked stale/cleared so consumers do not show a frozen "now playing".

**Notes / constraints** (for planning):
- Expose now-playing as a single observable snapshot (a `Flow<NowPlaying>`), built on the existing
  `manager.subscribe(...)` pattern, so both the in-app screen and the `003` notification read the
  *same* state. Transport actions invoked from the notification reuse the same `Commands` methods.

---

### Edge Cases

- **App reports no play-state** (empty `foregroundAppInfo` / missing `playState`): show app
  identity + "state unknown"; transport buttons act best-effort, not as confirmed toggles.
- **Foreground is an HDMI/external input**: show the input's label/icon (from
  `getExternalInputList` / launch points); no play-state or transport guarantees; hide
  channel/programme.
- **TV off / asleep / off-network**: the now-playing screen shows the same disconnected/off-network
  state as the rest of the app (Principle IV) and clears any stale snapshot.
- **Rapid app switching**: the live subscription may emit bursts; the UI must settle on the latest
  state without flicker and not queue stale frames.
- **mediaId changes but app stays the same** (e.g. autoplay next episode): treated as continued
  playback in the same app; we cannot show the new item's title (no metadata).
- **Subscription dropped on reconnect**: re-subscribe automatically when the socket returns; until
  then show "reconnecting", not the last-known state as if live.
- **Live-TV programme info missing/partial**: show channel only; never block the screen on EPG data.
- **Backgrounded app / process killed**: on return, re-establish subscriptions and show accurate,
  non-stale now-playing state.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST display the TV's current foreground app/source (human-readable name and
  icon, resolved from launch points / input list) on a now-playing surface.
- **FR-002**: The app MUST subscribe to live foreground-app updates
  (`ssap://com.webos.applicationManager/getForegroundAppInfo`) and reflect changes without a manual
  refresh, using the existing subscribe-over-the-live-socket pattern.
- **FR-003**: The app MUST obtain and display real playback state (playing / paused / stopped) where
  the TV reports it via `ssap://com.webos.media/getForegroundAppInfo`, and MUST explicitly indicate
  "unknown" when the TV does not report it.
- **FR-004**: The app MUST provide transport controls limited to what SSAP supports —
  **play/pause, stop, rewind, fast-forward** (`ssap://media.controls/*`) — and MUST NOT present
  next/previous or a seek/scrubber control, because SSAP does not support them.
- **FR-005**: The play/pause control MUST reflect real reported play-state when available, and fall
  back to the existing best-effort last-action toggle when play-state is unknown, indicating the
  state is unconfirmed.
- **FR-006**: When the foreground source is live TV, the app MUST display the current channel
  (`ssap://tv/getCurrentChannel`) and, where available, the current programme name
  (`ssap://tv/getChannelProgramInfo`); it MUST hide this block for non-live sources.
- **FR-007**: The app MUST NOT claim to show media title / artist / album / episode / artwork /
  elapsed position for streaming-app playback, since SSAP does not expose them; the now-playing
  surface degrades to app identity + play-state + transport controls.
- **FR-008**: The app MUST expose the live now-playing state as a single shared observable snapshot
  consumable by the `003` lockscreen / notification feature, so both surfaces reflect identical
  state and transport actions reuse the same command path.
- **FR-009**: The now-playing surface MUST honour the app-wide connection state (connected /
  connecting / disconnected / off-network) and MUST clear or mark stale any now-playing snapshot
  when the TV is unreachable, rather than showing frozen state (Principle IV).
- **FR-010**: The app MUST re-establish its now-playing subscriptions automatically after a
  reconnect, and surface "reconnecting" rather than presenting last-known state as live.
- **FR-011**: Transport actions MUST be sent directly to the TV over the existing secure SSAP socket
  with no backend or third party in the path (Principle III), reusing the `002` `Commands` layer.

### Key Entities *(include if feature involves data)*

- **NowPlaying snapshot**: the app's current understanding of what the TV is showing. Attributes:
  foreground app id + resolved display name + icon; source kind (app / live-TV / external-input);
  play-state (playing / paused / stopped / unknown); optional live-TV channel + programme; and a
  freshness flag (live / stale / cleared). Not persisted; derived from live subscriptions.
- **Transport command**: a single media-control action (play / pause / stop / rewind / fast-forward)
  sent to the TV. Not persisted; has a type and a best-effort result (acknowledged / unconfirmed).
- **Channel/programme info**: for live-TV sources only — channel name/number and current programme
  name from the TV's tuner/EPG. Absent for streaming apps and external inputs.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When the foreground app changes on the TV, the now-playing screen reflects the new app
  within 2 seconds, with no user action.
- **SC-002**: For apps that report play-state, the screen's playing/paused indicator matches the
  TV's actual state in 100% of observed pause/resume events (subject to the app reporting it).
- **SC-003**: For apps that do **not** report play-state, the app never shows a false
  "playing"/"paused" claim — it shows "unknown" instead.
- **SC-004**: The transport controls shown are exactly the SSAP-supported set (play/pause, stop,
  rewind, fast-forward) — no dead next/previous or scrubber controls are ever displayed.
- **SC-005**: On a live-TV channel, the channel (and programme name, when the TV provides it) is
  displayed; on a streaming app, no false title/metadata is shown.
- **SC-006**: The `003` lockscreen/notification and the in-app now-playing screen show identical app
  + play-state at all times (single shared snapshot).
- **SC-007**: When the TV becomes unreachable, the now-playing surface shows a disconnected/stale
  state within 5 seconds and never presents a frozen "now playing".

## Assumptions

- **SSAP metadata ceiling**: the TV exposes foreground-app identity and (often) play-state, plus
  live-TV channel/programme, but **no** rich media metadata for app playback — confirmed by the
  Research findings above. The feature is scoped to that ceiling and degrades gracefully.
- **Best-effort transport**: `media.controls` commands are honoured per app; the app reflects
  reported state and sends best-effort commands, not guaranteed outcomes.
- **Same direct-LAN architecture as `002`**: secure SSAP socket, custom TrustManager, no backend;
  this feature adds subscriptions + a screen, not new transport.
- **`003` is a separate feature**: this spec defines the shared now-playing state `003` consumes; it
  does not implement the lockscreen/notification UI itself.
- **Android-only, home-Wi-Fi-only**: inherits `002`'s platform and connectivity scope.
- **No persistence**: now-playing is live-only; nothing about what's playing is stored.

## Clarifications

### Session 2026-06-20

- **Rich metadata (title/artist/art) for apps**: **Not deliverable over SSAP** — confirmed by survey
  of go-webos, pywebostv, webostv and HA #91709. The screen shows app identity + play-state only for
  app playback; the one real "title" path is live-TV programme info. *(Resolved by research; no user
  input needed.)*
- **Next / previous controls**: **Out of scope** — no SSAP URI exists; offering them would create
  dead controls. Only play/pause, stop, rewind, fast-forward are exposed. *(Resolved by research.)*
- **Seek / scrubber**: **Out of scope** — no position reported, no seek URI. *(Resolved by research.)*
- **Relationship to `003`**: this feature owns the canonical live now-playing snapshot; `003`
  renders it on the lockscreen/notification and reuses the same `Commands` for transport.

### Open questions (for /clarify)

- **OQ1 — play/pause unification**: should the existing `002` blind `playPause()` be *replaced* by
  real-state-driven play/pause everywhere (including the main remote screen), or only on the
  now-playing surface? (Recommendation: drive both from the shared snapshot.)
- **OQ2 — surface form**: is now-playing a full screen, a bottom sheet, or a persistent card on the
  main remote? (Affects navigation and how `003` mirrors it.)
- **OQ3 — icon source for play-state-only apps**: when `com.webos.media` reports an `appId` not in
  `listLaunchPoints` (rare), what fallback icon/name do we show?
- **OQ4 — live-TV scope**: is live-TV channel/programme (US3) wanted at all for this user, or is the
  TV used almost entirely for streaming apps (making US3 droppable for v1)?
- **OQ5 — subscription cost**: running 2–3 standing subscriptions continuously vs only while the
  now-playing surface (or `003` notification) is active — any battery/connection concern worth
  gating on?
