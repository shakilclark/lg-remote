# Feature Specification: Swipe-up to reveal "More controls"

**Feature Branch**: `023-swipe-up-more-controls`

**Created**: 2026-06-21

**Status**: Implemented — swipe-up on the grip opens the command sheet (FR-001/002/003/005/007). The
upward drag commits the reveal once it passes a ~40dp threshold; FR-004 is met as a threshold-triggered
open rather than continuous finger-tracking of the sheet position (the sheet is an M3
`ModalBottomSheet`). Dismiss is the sheet's own swipe-down / scrim tap. The reveal uses
`ModalBottomSheet`'s default animation (same as the tap path); a dedicated reduce-motion settle (FR-006)
remains a follow-up.

**Relates to**: `010-expressive-redesign` (the `Grip` + `CommandSheet` / "More controls"),
`022-clickpad` (shares the home surface; gestures must be mutually disambiguated).

**Input**: User description: "swipe up to reveal 'more' controls."

## User Scenarios & Testing

### User Story 1 - Swipe up to raise the command sheet (Priority: P1)

From the home surface, the user **swipes up** (from the grip / bottom edge) to raise the command sheet
of "More controls" (Keypad / Apps / Inputs / Sound / Type) — in addition to the existing tap on the
grip handle. This matches the design's "pull up for everything else" promise with a real drag, not only
a tap.

**Why this priority**: The grip already implies a pull; making the swipe actually work is the natural,
discoverable gesture and the headline of this spec.

**Independent Test**: Swipe up from the grip area → the command sheet rises; tapping the grip still
also opens it.

**Acceptance Scenarios**:
1. **Given** the home surface, **When** the user swipes up from the grip/bottom edge, **Then** the
   command sheet is revealed.
2. **Given** the grip, **When** the user taps it, **Then** the sheet still opens (tap remains a valid
   affordance alongside swipe).
3. **Given** the sheet is open, **When** the user swipes down / taps the scrim, **Then** it dismisses.

### User Story 2 - Discoverable and non-conflicting (Priority: P2)

The grip handle stays the **visible affordance** (a handle that looks draggable). The swipe must not
collide with the `022` clickpad gestures: a swipe that **originates at the grip / bottom edge** reveals
the sheet, whereas taps and glides on the pad keep their clickpad meaning.

**Why this priority**: The gesture is only good if it's discoverable and never steals input from the
pad's nav/cursor.

**Acceptance Scenarios**:
1. **Given** the clickpad is active, **When** the user taps the bottom (down) zone, **Then** D-pad DOWN
   fires — not a sheet reveal.
2. **Given** the user starts a drag from the grip/bottom edge, **When** they move up, **Then** the sheet
   tracks the drag and settles open/closed past a threshold.

### Edge Cases

- A short/incomplete upward drag should settle back closed (threshold + fling velocity), not leave the
  sheet half-open.
- The reveal must respect reduce-motion (reduced/instant settle).
- The swipe origin zone must be reconcilable with the clickpad's bottom region and corner taps (see
  `022` FR-008).

## Requirements

### Functional Requirements

- **FR-001**: The home surface MUST reveal the command sheet on an upward swipe originating from the
  grip / bottom edge.
- **FR-002**: Tapping the grip MUST continue to open the sheet (swipe is additive, not a replacement).
- **FR-003**: The sheet MUST be dismissable by swipe-down / scrim tap.
- **FR-004**: The upward drag SHOULD track the finger and settle open/closed past a position/velocity
  threshold (no half-open resting state).
- **FR-005**: The swipe MUST be disambiguated from the `022` clickpad region taps, corner taps, and
  cursor glide so it never steals their input (and vice-versa).
- **FR-006**: The reveal/settle animation MUST respect reduce-motion.
- **FR-007**: The grip MUST remain a visible, accessible affordance (handle + `More controls`
  label/contentDescription) so the gesture is discoverable and operable without the gesture.

## Success Criteria

- **SC-001**: Users can open "More controls" by either swiping up from the grip or tapping it.
- **SC-002**: The swipe never triggers an unintended clickpad action (and clickpad taps never
  accidentally open the sheet).
- **SC-003**: Incomplete swipes settle cleanly (fully open or fully closed).

## Assumptions

- Builds on the existing `Grip` + `ModalBottomSheet` ("More controls") in `RemoteScreen`; this adds the
  drag-to-reveal gesture and keeps tap-to-open.
- Gesture-boundary reconciliation with `022-clickpad` is shared work; the swipe originating at the
  grip/bottom edge is the assumed disambiguator, to be confirmed in `/plan`.
