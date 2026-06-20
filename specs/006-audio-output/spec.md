# Feature Specification: Audio Output Switcher

**Feature Branch**: `006-audio-output`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (reuses the live SSAP connection, the
`manager.subscribe(...)` pattern, and the bottom-sheet UI vocabulary used by Inputs/Apps).

**Input**: User description: "spec: audio output switcher."

## Why (context)

A common real-world friction on an LG TV is moving sound between the **TV speakers**, an
**optical/ARC soundbar**, **Bluetooth**, or **headphones** — the physical magic remote buries
this several menus deep. webOS exposes the current sound output and a command to change it over
SSAP, so the app can offer a one-tap output switcher. It pairs naturally with the volume controls
already on the remote and with the now-playing surface (`004`).

## Research findings *(mandatory — what SSAP actually provides)*

- **Read current output**: `ssap://audio/getSoundOutput` returns the active output as a token,
  e.g. `{ "soundOutput": "tv_speaker" }`. It is **subscribable** (same mechanism as
  `audio/getVolume`), so the UI can reflect changes made elsewhere live.
- **Change output**: `ssap://audio/changeSoundOutput` with `{ "output": "<token>" }` switches it.
  The TV returns `returnValue:false` if that output isn't currently available.
- **Known output tokens** (webOS, observed across go-webos / pywebostv / Home Assistant):
  `tv_speaker`, `external_optical`, `external_arc` (HDMI ARC/eARC), `lineout`, `headphone`,
  `bt_soundbar` (Bluetooth), `tv_external_speaker` (optical + TV), `tv_speaker_headphone`,
  `soundbar`. Tokens vary slightly by firmware.
- **Enumerating *available* outputs is NOT reliable over SSAP.** There is no dependable "list the
  outputs this TV currently supports" call. The honest design is therefore: present the **known set
  of common outputs**, mark the one `getSoundOutput` reports as **active**, and let
  `changeSoundOutput`'s `returnValue` be the source of truth — surface a clear message when the TV
  rejects an unavailable output rather than pretending to know in advance.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See and switch the current audio output (Priority: P1)

While connected, I open the audio-output switcher, see which output is currently active, and tap a
different one (e.g. switch from TV speakers to my soundbar). The TV switches and the UI reflects it.

**Why this priority**: This is the whole feature — switching where sound goes in one tap instead of
digging through TV menus.

**Independent Test**: With a paired TV, open the switcher, confirm the active output is highlighted,
tap another available output, and hear/see the TV switch; the highlight moves to the new output.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I open the audio-output switcher, **Then** I see a list of
   common outputs with the **currently active** one clearly marked.
2. **Given** the switcher is open, **When** I tap a different, available output, **Then** the TV
   switches to it and the active marker moves to my selection.
3. **Given** the output is changed elsewhere (physical remote / another app), **When** that happens
   while the switcher is open, **Then** the active marker updates live (subscription).
4. **Given** I tap an output the TV cannot use right now, **When** the TV rejects it, **Then** the
   app shows a clear, non-alarming message ("That output isn't available right now") and the active
   marker does not move.

---

### User Story 2 - Reach it from the remote without clutter (Priority: P2)

I can open the audio-output switcher quickly from the remote, and it doesn't add visual noise to the
main screen when I'm not using it.

**Why this priority**: Discoverability matters, but audio-output switching is occasional, so it
should live behind a tap (a sheet), consistent with Inputs/Apps — not occupy permanent screen space.

**Independent Test**: From the connected remote, open the switcher in one tap from its entry point,
and confirm the main remote layout is unchanged when it's closed.

**Acceptance Scenarios**:

1. **Given** the connected remote, **When** I trigger the audio-output entry point, **Then** the
   switcher opens as a bottom sheet (same pattern as Inputs/Apps).
2. **Given** the sheet is open, **When** I pick an output or swipe down, **Then** it dismisses and
   returns me to the remote.

> **Open design question (entry point)**: the bottom bar is full (Back · Home · Pad · Apps · Inputs ·
> Settings). Candidates for triggering this switcher: (a) a long-press on the **Mute** key in the
> volume strip; (b) an action inside the **now-playing** surface (`004`); (c) folding audio output
> into the **Inputs** sheet as a second section ("Sources" + "Sound out"). To be decided in
> `/clarify`; it does not change the core feature.

---

### Edge Cases

- **Output rejected / unavailable** → clear message, no state change (US1 #4).
- **Disconnected while sheet open** → the sheet reflects the not-connected state and selection is
  disabled (consistent with how commands are gated when not connected, `002` FR-014).
- **Unknown token reported** → if `getSoundOutput` returns a token not in the known set, show it as a
  labelled "active" entry rather than hiding it, so the user still sees the truth.
- **No subscription support on a given firmware** → fall back to reading once on open.

## Requirements *(mandatory)*

- **FR-001**: The app MUST read the current sound output via `ssap://audio/getSoundOutput` when the
  switcher opens and reflect it as the active selection.
- **FR-002**: The app MUST subscribe to the sound-output state (reusing the `manager.subscribe`
  pattern) so the active marker stays correct when the output changes elsewhere; it MUST fall back to
  a one-shot read if subscription is unavailable.
- **FR-003**: The app MUST switch output via `ssap://audio/changeSoundOutput` with the selected
  token.
- **FR-004**: The app MUST present a curated list of common outputs with human-readable labels and
  icons (TV speaker, Optical, HDMI ARC, Bluetooth, Headphones, Line out, Soundbar).
- **FR-005**: When `changeSoundOutput` returns `returnValue:false` (or errors), the app MUST surface a
  clear message and leave the active marker unchanged (never silently fail — `002` Principle).
- **FR-006**: The switcher MUST open and close as a bottom sheet, matching the Inputs/Apps pattern;
  the main remote layout MUST be unchanged when it is closed.
- **FR-007**: Output switching MUST only be attempted while connected; otherwise it is disabled with
  a visible reason.
- **FR-008**: Labels/tokens MUST be defined in one place (a pure, unit-tested mapping) so they're easy
  to extend as new firmware tokens appear.

### Key Entities

- **SoundOutput**: `{ token: String, label: String, icon }` — a selectable output. The active one is
  whatever `getSoundOutput` currently reports.

## Success Criteria *(mandatory)*

- **SC-001**: From the connected remote, a user can change the TV's audio output in **≤ 2 taps**
  (open switcher → pick output).
- **SC-002**: The active output shown always matches the TV (verified by changing it from the
  physical remote and seeing the app update).
- **SC-003**: Choosing an unavailable output produces a clear message and no false state change.
- **SC-004**: The feature adds **zero** permanent elements to the main remote when not in use.
- **SC-005**: The token↔label mapping is covered by unit tests; the network calls are verified on the
  real TV (Principle V).

## Assumptions

- The TV is already paired/connected (`002`); this feature adds no new connection or permission.
- Presenting a known output set (rather than a TV-reported available list) is acceptable, with
  graceful rejection handling, because SSAP cannot reliably enumerate available outputs.
- Entry point is a small, later decision (see US2 open question) and does not block the core work.

## Clarifications

- **Entry point** (long-press Mute vs now-playing vs Inputs "Sources" section) — to resolve in
  `/clarify`.
- Whether to show **all** known outputs always, or hide clearly-irrelevant ones (e.g. headphone when
  none attached) — default is show-all-with-graceful-rejection, since availability isn't queryable.
