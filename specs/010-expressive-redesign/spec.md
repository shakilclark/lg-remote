# Feature Specification: Expressive Redesign (Direction C)

**Feature Branch**: `010-expressive-redesign`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the native app, SSAP client, `TvConnectionManager`,
motion-cursor engine). **Restyles / re-presents** the surfaces specified in `004-now-playing`,
`006-audio-output`, `008-seamless-reconnect`, and `009-notification-controls` — it changes how they
look and where they live, not the underlying SSAP behaviour those specs define.

**Input**: A full **Material 3 Expressive + Material You** restyle and restructure replacing the
electric-red, dark-only, D-pad + bottom-bar UI with the "Direction C" model: a calm gesture-pad home
and a pull-up command sheet, themed by Material You (Ultraviolet #7B2FF7 fallback) in light **and**
dark.

## Why this feature (context)

The current UI (`002`/`005`) is a brand-locked electric-red, **dark-only** remote with a D-pad cluster
and a five-tab bottom bar. It works, but it (a) ignores the user's system theme and Android's dynamic
colour, (b) crowds the home screen with buttons, and (c) predates the now-playing / audio-output /
notification surfaces, which were bolted on rather than designed in. This feature resets the design to
**Material 3 Expressive** — the platform's current, research-backed language — and restructures the app
around a single thumb-first gesture surface, with everything else one pull away. It is a UI
restyle/restructure: it MUST NOT regress pairing, the SSAP protocol, `TvConnectionManager`, or the
motion-cursor engine. The visual source of truth is `docs/design-system.md` and the reference artifacts
under `docs/design/`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - One calm surface that does the 80% (Priority: P1)

As the everyday driver, I open the remote to a single quiet surface: I glide my thumb to move the TV
pointer and tap to click; I swipe the right edge to change volume and the left edge to change channel;
a short swipe in from the edge is Back. I never hunt through a grid of buttons for the common things.

**Why this priority**: This is the redesign's core thesis — the home screen *is* the remote. Without it
the restructure has no value.

**Independent Test**: On a paired, connected TV, the home surface points/clicks via the existing
motion-cursor engine, and edge swipes drive volume/channel through the existing `Commands`, with no
button grid required.

**Acceptance Scenarios**:

1. **Given** a connected TV, **When** I glide on the pad and tap, **Then** the TV pointer moves and
   clicks (same behaviour as the current touchpad).
2. **Given** a connected TV, **When** I swipe the right edge up/down, **Then** volume goes up/down; the
   left edge changes channel; holding ramps.
3. **Given** any state, **When** I short-swipe in from the screen edge, **Then** Back is sent.

### User Story 2 - Pull up everything else (Priority: P1)

When I need the number pad, an app, an input, audio output, or the keyboard, I drag the visible grip at
the bottom and a command sheet rises with a segmented switch (Keypad / Apps / Inputs / Sound / Type). I
discover the depth by pulling a handle, not by memorising a gesture.

**Why this priority**: A full-time replacement must cover every physical-remote function; this is where
they live without crowding the home surface.

**Independent Test**: Dragging/tapping the grip opens the sheet; each segment shows its controls
(number pad + colour keys, app launcher, input switcher, audio output, on-screen keyboard) wired to the
existing commands.

**Acceptance Scenarios**:

1. **Given** the home surface, **When** I drag or tap the grip, **Then** the command sheet opens to its
   last-used segment.
2. **Given** the sheet open, **When** I tap a segment, **Then** that segment's controls show and act on
   the TV.
3. **Given** a first run, **When** the home surface first appears, **Then** a one-time card teaches the
   two edge gestures and is dismissible and never shown again.

### User Story 3 - Themed to my phone, light or dark (Priority: P1)

The remote follows my system light/dark setting and, on Android 12+, takes its colour from my wallpaper
(Material You). When dynamic colour is unavailable, it falls back to a distinctive Ultraviolet palette.
Either way contrast is strong and the app looks native to my phone.

**Why this priority**: Looking native and respecting the system theme is the most visible promise of the
redesign and applies to every screen.

**Independent Test**: Toggling system dark mode flips the app; on Android 12+ changing the wallpaper
palette re-tints it; on older devices the Ultraviolet fallback renders; all text meets WCAG AA.

**Acceptance Scenarios**:

1. **Given** any device, **When** the system is in light or dark mode, **Then** the app matches it.
2. **Given** Android 12+ with dynamic colour on, **When** the wallpaper palette changes, **Then** the
   app re-tints from it.
3. **Given** an older device or dynamic colour off, **When** the app launches, **Then** the Ultraviolet
   fallback palette is used.

### User Story 4 - Now-playing, honestly (Priority: P2)

When something is playing, a now-playing bar appears on the home surface; tapping it opens an immersive
cover view with transport controls. I see the app's own icon/tile; for sources that actually provide art
(Live TV channel logo, music, DLNA) I see that art — but the app never invents album art, titles, a
scrubber, or next/previous it cannot deliver. *(Re-presents `004`.)*

**Why this priority**: A high-value surface, but secondary to the core remote and theming.

**Acceptance Scenarios**:

1. **Given** media playing, **When** I look at the home surface, **Then** a now-playing bar shows the
   app icon, name, and play-state.
2. **Given** the now-playing bar, **When** I tap it, **Then** a cover view opens with play/pause, stop,
   rewind, fast-forward and a sound-output chip.
3. **Given** a source with real art, **When** the cover view opens, **Then** the artwork is shown; else
   the app tile is shown — never a fabricated image.

### User Story 5 - Switch where the sound goes (Priority: P2)

Tapping the output chip opens a picker of audio outputs (TV speakers, soundbar, Bluetooth, headphones,
optical); the active one is checked, and an output is only shown unavailable after the TV rejects it.
*(Re-presents `006`.)*

**Acceptance Scenarios**:

1. **Given** connected, **When** I open the output picker, **Then** the active output is checked.
2. **Given** the picker, **When** I tap another output, **Then** the TV switches and the chip updates; if
   the TV rejects it, that output is marked unavailable with a clear message.

### User Story 6 - Control from the lock screen (Priority: P2)

A media notification on the lock screen and shade lets me mute, change volume, and play/pause without
opening the app, in a five-button "full player" layout (mute · vol− · play/pause · vol+ · stop), with
three promoted in the collapsed view. It shows the same now-playing info and has no progress bar (no
position is available). *(Re-presents `009`; depends on `008` Tier-2 service.)*

**Acceptance Scenarios**:

1. **Given** a connected TV and the app opened once, **When** the phone is locked, **Then** a media
   notification with the five controls is present and works without unlocking.
2. **Given** the collapsed notification, **When** I view it, **Then** vol− / play-pause / vol+ are
   promoted.

### User Story 7 - Lock/unlock is a non-event (Priority: P3)

Returning to the app after a lock/background shows the remote already there and usable; the connection
state is a small top chip (Connected / Resuming… / Off network), and the full "can't reach your TV"
screen appears only after a real, sustained failure. *(Re-presents `008`.)*

**Acceptance Scenarios**:

1. **Given** a connected session, **When** I lock then unlock within the grace window, **Then** no
   disconnect screen flashes and the chip shows "Resuming…" at most.
2. **Given** the TV is genuinely off/unreachable past the grace window, **When** I return, **Then** the
   chip shows "Off network" and the reconnect screen appears.

### User Story 8 - Expressive polish (Priority: P3)

Every control feels tactile and current: Material Symbols Rounded icons (filling on selection), spring
motion, shape-morphing on press, the gesture pad as a recessed/textured well, and tonal elevation
instead of heavy drop shadows — consistent in light and dark.

**Acceptance Scenarios**:

1. **Given** any control, **When** I press it, **Then** it responds with spring scale + shape morph +
   haptic.
2. **Given** the home surface, **When** I view it in light or dark, **Then** the gesture pad reads as a
   recessed well and icons are the Rounded Material Symbols set.

### Edge Cases

- Dynamic colour unavailable (pre-Android-12, or user disabled) → Ultraviolet fallback, both modes.
- A wallpaper palette with poor contrast → the app MUST still meet AA (clamp/adjust roles, don't ship a
  low-contrast scheme).
- Reduce-motion enabled → shape-morph/press springs and the live pulse degrade to simple/again state
  changes; no essential information conveyed by motion alone.
- The command sheet opened while disconnected → controls reflect the unavailable state, don't silently
  no-op.
- Gesture discoverability → the one-time teaching card covers the two non-obvious edge gestures; the grip
  is always visible so the sheet is never "hidden".
- Notification present but TV turned off → controls fail visibly per Principle IV, not silently.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The home surface MUST be a single gesture pad providing pointer move + tap-to-click via the
  existing motion-cursor engine.
- **FR-002**: Edge gestures MUST drive volume (right edge) and channel (left edge) with hold-to-repeat,
  and a swipe-in from the edge MUST send Back.
- **FR-003**: A persistently visible grip MUST open a command sheet with segments Keypad, Apps, Inputs,
  Sound, and Type, each wired to the existing TV commands.
- **FR-004**: A one-time, dismissible first-run card MUST teach the volume/channel edge gestures.
- **FR-005**: The app MUST support full light and dark colour schemes and follow the system setting by
  default, with a manual override.
- **FR-006**: On Android 12+, the app MUST use Material You dynamic colour when available; otherwise it
  MUST use a fixed Ultraviolet (#7B2FF7) fallback palette. Either way, text/icon contrast MUST meet
  WCAG AA.
- **FR-007**: Iconography MUST use the Material Symbols Rounded set, using the fill state for
  selected/active controls.
- **FR-008**: Controls MUST give tactile feedback (spring scale + shape morph + haptic on press) and the
  app MUST use tonal elevation rather than heavy drop shadows; the gesture pad MUST read as a recessed
  surface in both light and dark.
- **FR-009**: A now-playing bar MUST appear on the home surface when media is playing and open an
  immersive cover view with transport controls (play/pause, stop, rewind, fast-forward).
- **FR-010**: Now-playing MUST show real artwork only when the source provides it (e.g. Live TV channel
  logo); otherwise the app tile. It MUST NOT display fabricated album art, titles, a scrubber, or
  next/previous controls that SSAP cannot supply.
- **FR-011**: A sound-output chip MUST open an output picker that marks the active output and marks an
  output unavailable only after the TV rejects it.
- **FR-012**: A lock-screen/shade media notification MUST offer the five-button player (mute · vol− ·
  play/pause · vol+ · stop), promoting three in the collapsed view, mirroring the now-playing snapshot,
  with no progress bar.
- **FR-013**: Connection state MUST be presented as a top chip (Connected / Resuming… / Off network); a
  lock/unlock within the grace window MUST NOT show a disconnect screen.
- **FR-014**: The redesign MUST NOT regress pairing, the SSAP protocol, `TvConnectionManager`, or the
  motion-cursor engine; all existing user stories (US1–US7 of `002`) MUST remain functional.
- **FR-015**: Motion and the live pulse MUST respect the system "reduce motion" setting.

### Key Entities

- **Theme/ColorScheme**: the resolved light or dark scheme, sourced from dynamic colour or the
  Ultraviolet fallback; carries the M3 role tokens consumed by every surface.
- **CommandSheet segment**: one of Keypad / Apps / Inputs / Sound / Type; the unit the sheet switches
  between.
- **NowPlaying snapshot**: app id → resolved name + icon + play-state (from `004`), shared by the
  in-app surfaces and the notification; optional source-provided artwork.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From the home surface, the four most common actions (point/click, volume, channel, back)
  are doable with **zero navigation** — no screen change or button-grid hunt.
- **SC-002**: Every non-home control (numbers, apps, inputs, sound, keyboard) is reachable in **one
  gesture** (a single grip pull) from the home surface.
- **SC-003**: The app renders correctly and legibly in **both** light and dark, with all text meeting
  **WCAG AA** contrast, on a dynamic-colour device and on the Ultraviolet fallback.
- **SC-004**: A first-time user can identify and use volume, channel, and "more controls" **within the
  first session** without external help (the teaching card + visible grip suffice).
- **SC-005**: A lock → unlock cycle within the grace window shows **no** disconnect screen and the
  remote is usable **immediately** on return.
- **SC-006**: 100% of media-state and connection information shown is real (no fabricated artwork,
  title, position, or output availability) — verified against what SSAP actually returns.
- **SC-007**: No regression: pairing and all `002` control functions pass their existing tests/goldens
  after the restyle.

## Assumptions

- The motion-cursor engine, SSAP `Commands`, `TvConnectionManager`, and pairing from `002` are reused
  unchanged; this feature re-skins and re-routes their UI, not their logic.
- The behavioural specs for now-playing (`004`), audio output (`006`), seamless reconnect (`008`), and
  notification controls (`009`) define what those surfaces *do*; `010` defines how they look and where
  they live in the new structure, and is built on top of them.
- Material 3 Expressive APIs are available via a Compose dependency bump; the **look** degrades
  gracefully if a given Expressive component is unavailable (custom equivalents already exist in the
  codebase, e.g. `pressMorph`).
- Portrait, one-handed phone use remains the only supported form factor (per the constitution).
- Light/dark follows the system by default; a manual in-app override is in scope, a per-time-of-day
  auto-schedule is not.
