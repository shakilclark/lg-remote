# Feature Specification: Clickpad — cursor + tap-to-navigate touchpad

**Feature Branch**: `022-clickpad`

**Created**: 2026-06-21

**Status**: Built (2026-06-21). Resolved design (workshopped in an artifact): one surface, tap vs drag
arbitrated by `touchSlop` in a single `awaitEachGesture`; **drags routed by start zone** — a slim
drag-only **volume rail** on the right edge (between the TR/BR corners) = volume, glide everywhere
else = cursor (volume also on the phone hardware buttons). Four corners = **TL TV-Settings, TR Mute,
BL Back, BR Home**; the empty TL corner took TV-Settings, which (with Back/Home/Mute) emptied the
command-sheet quick row — now removed. Tap edges = nav, centre = OK; faint hints surface on touch;
zones exposed as custom accessibility actions.

> **Design update (M3 Expressive):** the pad's original skeuomorphic "rubber clicker" treatment
> (recessed/embossed hints, mesh texture, inset well) was tried and **dropped** in favour of Material 3
> Expressive conventions — a **flat tonal surface** (`surfaceContainerHigh`) with an M3 filled-tonal,
> shape-morphing OK key and **faint, low-emphasis** hints that brighten on touch. US4 / FR-004 below
> reflect this; the skeuomorphic attempt is preserved at git tag `experiment/skeuomorphic-clickpad`.

**Supersedes**: `019-trackpad-back` (which scoped only back-control options on the trackpad — this spec
defines the full corner-action + tap-nav model, of which Back is one part).

**Relates to**: `010-expressive-redesign` (the `GesturePad` home surface, motion-cursor engine),
`023-swipe-up-more-controls` (the swipe that reveals the command sheet over this same surface).

**Input**: User description: "'clickpad' touchpad that folds in cursor controls. up/down/left/right
taps navigate, bottom-left corner tap = back, bottom-right = home, top-right = mute. Touchpad should be
clear, while interacting with it expose the control hints. Make control hints more subtle, recessed
like into a rubber clicker. Move duped controls from 'More controls'."

## User Scenarios & Testing

### User Story 1 - Tap a region to navigate (Priority: P1)

The home surface is one large **clickpad**. A discrete tap in the **top / bottom / left / right** zone
sends the matching D-pad direction to the TV; a tap in the **centre** is OK/select. This makes the most
common TV action — directional navigation — possible without aiming at small buttons.

**Why this priority**: Directional nav is the single most-used remote action; folding it into the pad
as region-taps is the core of the clickpad.

**Independent Test**: Tap each zone, confirm the correct D-pad button (`UP/DOWN/LEFT/RIGHT/OK`) is sent
once per tap.

**Acceptance Scenarios**:
1. **Given** the remote is connected, **When** the user taps the upper zone, **Then** exactly one
   `UP` is sent (and likewise down/left/right/centre→OK).
2. **Given** a tap lands near a zone boundary, **When** it resolves, **Then** it maps to a single
   unambiguous direction (no double-send).

### User Story 2 - Corner actions: Back / Home / Mute (Priority: P1)

Tapping the **bottom-left** corner = Back, **bottom-right** = Home, **top-right** = Mute. These pin the
three next-most-common actions to fixed, muscle-memory corners on the same surface.

**Why this priority**: Removes the need for a separate button row for the most-used non-directional
actions.

**Acceptance Scenarios**:
1. **Given** the pad, **When** the user taps the bottom-left corner, **Then** Back is sent; bottom-right
   → Home; top-right → Mute (toggles, with feedback).
2. **Given** a corner tap, **When** it resolves, **Then** it does NOT also fire the adjacent edge
   direction.

### User Story 3 - Cursor controls fold into the same pad (Priority: P1)

The pad still **folds in the pointer**: a sustained **glide** moves the Magic-Remote-style cursor
(reusing the existing motion-cursor / pointer engine); a discrete tap clicks. Tap-nav and glide-cursor
coexist on one surface, disambiguated by movement (a quick stationary touch = region tap/click; a
sustained drag = cursor move).

**Why this priority**: Some webOS apps need a pointer; the clickpad must serve both models without a
mode switch.

**Acceptance Scenarios**:
1. **Given** the pad, **When** the user drags, **Then** the cursor moves and no region-nav fires.
2. **Given** the pad, **When** the user taps without moving, **Then** region-nav/click fires and the
   cursor does not jump.

### User Story 4 - Faint hints, revealed on touch (Priority: P2)

At rest the pad is **clear** — the direction/corner hints are **very subtle, low-emphasis** glyphs on
the flat tonal surface (barely-there, `onSurfaceVariant` at low alpha). On **touch-down** the hints
**brighten** to confirm the zones, then recede when the finger lifts. (Per the M3 Expressive update,
this replaces the original embossed "rubber clicker" treatment — the surface is flat and tonal, depth
comes from colour, not skeuomorphic shadow.)

**Why this priority**: Keeps the calm, uncluttered resting surface (the design's core idea) while still
teaching the zones at the moment of use. Refines the current always-on edge hints.

**Acceptance Scenarios**:
1. **Given** the pad at rest, **When** no touch is active, **Then** hints are minimal/low-emphasis (not
   bright overlays).
2. **Given** the user touches the pad, **When** the touch begins, **Then** the zone hints become
   visible, and **When** released, **Then** they recede.
3. **Given** reduce-motion is on, **When** hints reveal, **Then** they cross-fade minimally / snap (no
   large motion).

### User Story 5 - Remove duplicated controls from "More controls" (Priority: P2)

Because Back / Home / Mute now live on the clickpad, the duplicate **Back / Home / Mute** entries in the
command sheet's quick-action row ("More controls") are **removed**, leaving only what isn't on the pad
(e.g. Settings). No control is offered in two places.

**Why this priority**: Avoids redundant, confusing duplication once the pad carries these actions.

**Acceptance Scenarios**:
1. **Given** the command sheet, **When** it opens, **Then** Back/Home/Mute are not duplicated there
   (they live on the pad); remaining quick actions (e.g. Settings) stay.

### Edge Cases

- Tap vs glide disambiguation threshold (movement distance / dwell) — must avoid accidental nav while
  starting a cursor drag, and avoid cursor drift on a tap.
- Corner zones vs adjacent edge zones — corners win within their hit area; define sizes so neither
  starves the other.
- Mute is a toggle — needs clear on/off feedback (icon/haptic), since it's colour-state on the pad.
- Accessibility: each zone/corner needs a `Role.Button` + contentDescription and a ≥48dp effective
  target; TalkBack users get explicit buttons, not an unlabelled surface.
- Interaction with `023` swipe-up: a vertical swipe originating at the grip/bottom edge reveals the
  command sheet and must NOT be consumed as a down-region tap or a cursor glide.

## Requirements

### Functional Requirements

- **FR-001**: The home clickpad MUST map discrete region taps to D-pad directions (top→UP, bottom→DOWN,
  left→LEFT, right→RIGHT, centre→OK), one button per tap.
- **FR-002**: The clickpad MUST map corner taps: bottom-left→Back, bottom-right→Home, top-right→Mute
  (toggle, with feedback).
- **FR-003**: The clickpad MUST also support glide-to-move-cursor + tap-to-click (the existing pointer
  engine), disambiguated from region taps by movement.
- **FR-004**: At rest the pad's hints MUST be subtle/low-emphasis (faint glyphs on the flat tonal M3
  surface — not the dropped skeuomorphic "rubber clicker" treatment), and MUST brighten on touch-down
  and recede on release.
- **FR-005**: Hint reveal/recede MUST respect reduce-motion.
- **FR-006**: Back / Home / Mute MUST be removed from the command-sheet quick-action row once present on
  the pad; no action appears in two places.
- **FR-007**: Every tap zone and corner MUST expose accessible semantics (`Role.Button` +
  contentDescription) and an effective target ≥48dp.
- **FR-008**: Region taps, corner taps, cursor glide, and the `023` swipe-up MUST be mutually
  disambiguated so each gesture fires only its intended action.

## Success Criteria

- **SC-001**: Users can perform UP/DOWN/LEFT/RIGHT/OK and Back/Home/Mute entirely from the pad, with no
  separate button row.
- **SC-002**: At rest the pad reads as a calm, near-unmarked surface; on touch the active zones are
  legible.
- **SC-003**: In normal use, taps vs glides are correctly distinguished ≥95% of the time (no accidental
  nav when starting a cursor drag; no drift on a tap).
- **SC-004**: No control is duplicated between the pad and the command sheet.

## Assumptions

- Builds directly on the `010` `GesturePad` and the existing motion-cursor / pointer-button-injection
  paths (region nav and corners reuse `NavButton` injection; glide reuses the cursor engine).
- The current always-on edge VOL hint and bottom-left Back corner are folded into this unified hint +
  corner model; this spec supersedes `019-trackpad-back`.
- Volume: **hardware buttons only** (kept off the pad). A right-edge drag rail was tried but, on device,
  it collided with the **Right tap-zone** (a slightly-imperfect "tap right" read as a volume drag), so it
  was dropped — the right edge is now unambiguous Right-nav. Volume stays reachable on the phone's
  hardware keys and as TalkBack custom actions. (Research: no shipping TV remote puts volume on the pad.)
- Resolved zone geometry: 3×3 with corner hit-boxes (30%×24%), a central OK dead-zone (radius 0.16),
  and the rest by angle from centre; centre tap = the pointer click (`onClick`). Built; thresholds tuned
  live in the workshop artifact.
