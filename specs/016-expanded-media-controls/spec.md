# Feature Specification: Expanded Media Controls in Now-Playing

**Feature Branch**: `016-expanded-media-controls`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `004-now-playing` (the now-playing surface + transport), `010-expressive-redesign`
(the cover view), and `002-native-android-remote` (the SSAP client + the **pointer input socket**
already opened for the motion cursor).

**Input**: "Expanded media controls in now-playing." Research-first.

## Why this feature (context)

The now-playing / cover view today offers only the basic transport (play/pause, stop, rewind,
fast-forward). A real viewer reaches for more — above all **captions** — and webOS can actually deliver
several of these. The point of this feature is to add the genuinely-supported extras **without
cluttering** the primary transport and **without pretending** to controls webOS can't honour.

### Research findings (what webOS/SSAP actually supports)

Two mechanisms:

1. **`ssap://media.controls/*`** — `play`, `pause`, `stop`, `rewind`, `fastForward`. High-level, app-
   aware where the app implements it. (No `seek`/position; no `next`/`prev` here.)
2. **Button injection over the pointer input socket** (the same socket the app already uses for the
   motion cursor) — exposes the full remote keyset, including: **CC** (captions), **INFO**, **PROGRAM**
   (guide), **GOTOPREV** / **GOTONEXT** (skip), **RED/GREEN/YELLOW/BLUE**, **AD** (audio description),
   **RECORD**, **FAVORITES**, **QMENU** (quick menu), **LIST**, **0–9**.

Hard caveats (honesty):

- Button injection is **fire-and-forget**: the TV doesn't report whether the foreground app acted on it,
  so the UI MUST NOT show a confirmed on/off state it cannot read (e.g. captions show as a *toggle
  request*, not a guaranteed "captions ON").
- **App-dependent**: `GOTOPREV/GOTONEXT` and even play/pause behave differently per app; some no-op.
  Present them as best-effort.
- Still **no scrubber / seek-to-position** (no position is reported) and no reliable next/prev *state*.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Toggle captions from now-playing (Priority: P1)

While watching, I tap a captions (CC) control in the now-playing/cover view and the TV toggles
subtitles — without leaving the remote or digging through TV menus.

**Why this priority**: Captions are the single most-reached-for "extra" during playback; they're cheap
to send (CC button) and high value.

**Acceptance Scenarios**:

1. **Given** connected with media playing, **When** I tap CC, **Then** the TV receives the CC button
   and toggles captions.
2. **Given** the fire-and-forget nature, **When** I tap CC, **Then** the control shows it *sent* a
   toggle (brief press feedback), not a fabricated persistent "ON" state.

### User Story 2 - Skip back / forward (best-effort) (Priority: P2)

In the cover view I can skip to the previous/next item where the app supports it (GOTOPREV/GOTONEXT),
alongside rewind/fast-forward.

**Acceptance Scenarios**:

1. **Given** an app that supports skip, **When** I tap skip-next, **Then** it advances; **Given** an app
   that doesn't, **Then** nothing happens and the UI never claimed it would for sure.

### User Story 3 - A "more" overflow for the rest (Priority: P2)

The primary transport row stays clean (rewind · play/pause · fast-forward, with stop); the remaining
expanded controls — INFO, Guide, Record, the four colour buttons, Audio description, Quick menu — live
in a **more** overflow so they're available without crowding.

**Acceptance Scenarios**:

1. **Given** the cover view, **When** I open "more", **Then** I see the secondary controls grouped and
   labelled; the primary row remains uncluttered.

### User Story 4 - Honest, uncluttered, restrained (Priority: P1, cross-cutting)

Expanded controls never imply state the app can't read, never crowd the primary transport, and degrade
gracefully when disconnected.

**Acceptance Scenarios**:

1. **Given** any expanded control, **When** shown, **Then** it is presented as an action (a button
   press), not a confirmed toggle/state, unless webOS actually reports that state.
2. **Given** disconnected, **When** I open now-playing, **Then** expanded controls reflect the
   unavailable state rather than silently no-op.

### Edge Cases

- Live TV vs streaming app → some controls (Guide, channel, Record) are meaningful only for Live TV;
  show/hide or disable by context where the foreground app is known.
- Rapid taps → button injections are debounced enough not to flood the socket, but each press still
  gives immediate local feedback.
- An app that ignores a button → no error surfaced for a best-effort control; the user simply sees no
  TV change (documented behaviour).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Add a **captions (CC)** control to the now-playing/cover view that sends the CC button
  over the pointer input socket; present it as a toggle *request* (press feedback), not a confirmed
  state.
- **FR-002**: Add best-effort **skip previous / skip next** (GOTOPREV/GOTONEXT) to the cover view
  transport area, clearly best-effort.
- **FR-003**: Provide a **"more" overflow** containing INFO, Guide (PROGRAM), Record, the four colour
  buttons, Audio description (AD), and Quick menu (QMENU); keep the primary transport row to
  rewind · play/pause · fast-forward (+ stop).
- **FR-004**: All expanded controls MUST reuse the existing pointer-input-socket button-injection path
  (no new transport mechanism); commands are fire-and-forget.
- **FR-005**: The UI MUST NOT display confirmed media state (captions on/off, current track) that webOS
  does not report; expanded controls render as actions unless a real subscribable state exists.
- **FR-006**: Context-sensitivity: where the foreground app/source is known, Live-TV-only controls
  (Guide, Record, channel) SHOULD be shown/enabled only in that context.
- **FR-007**: Expanded controls MUST respect the redesign's restraint (FR from 012): the primary row
  stays minimal; extras live in overflow; no clutter.

### Key Entities

- **Expanded control**: a labelled action mapped to either a `media.controls/*` URI or a pointer-socket
  **button name** (CC, INFO, PROGRAM, GOTOPREV/NEXT, RED/GREEN/YELLOW/BLUE, AD, RECORD, FAVORITES,
  QMENU), tagged best-effort/fire-and-forget.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Captions can be toggled from now-playing on a real TV in one tap.
- **SC-002**: The primary transport row gains **no** extra buttons beyond the existing transport; all
  additions live in CC + skip + a "more" overflow.
- **SC-003**: No expanded control displays a confirmed state the TV doesn't report (audited against the
  fire-and-forget caveat).
- **SC-004**: Live-TV-only controls do not appear for streaming-app playback (and vice-versa) where the
  foreground app is known.

## Assumptions

- The native SSAP client can send button events over the already-open pointer input socket (it powers
  the motion cursor today); no new connection is needed.
- "Honest, best-effort" is acceptable to the user (consistent with 004's stance) — we expose what the
  remote's physical buttons do, with the same lack of confirmation.
- Per-app capability detection beyond "which app is foreground" is out of scope; context rules use the
  foreground-app id only.
