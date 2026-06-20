# Feature Specification: Trackpad Back Control — Design Options

**Feature Branch**: `019-trackpad-back`

**Created**: 2026-06-20

**Status**: Draft — options for decision

**Relates to**: `010-expressive-redesign` / the `GesturePad`, and the deferred home-inversion (where the
gesture pad becomes the primary surface and Back is no longer a persistent bottom-bar button).

**Input**: "Trackpad back control design options." Research-first.

## Why this feature (context)

On the gesture-pad home, **single-finger drag drives the pointer** and the **left/right edges are
channel/volume** — so the obvious mobile gesture for Back (an inward edge-swipe) is already taken, and
the obvious trackpad gesture (a single swipe) collides with pointer movement. Yet **Back is one of the
most-used controls on a TV remote**, so it must be fast *and* findable. Research is blunt about the
failure mode: touchpad remotes that hide Back behind an invisible gesture (e.g. the Siri Remote) push
people back to the physical remote — the exact thing this app exists to replace. So the design must
pick an input that **doesn't conflict** with move / edge-rockers **and** keep a **visible** Back. This
spec lays out the options and a recommendation; it's a decision doc, not a single mandate.

### Research summary

- **Trackpad convention**: two-finger horizontal swipe = back/forward (macOS); multi-finger gestures
  are the established "secondary navigation" space because they don't collide with single-finger move.
- **Android**: inward edge-swipe = back — but our edges are volume/channel, so that exact mapping is
  unavailable.
- **TV/remote**: Back is expected to be utterly reliable and consistent; touchpad-only remotes that
  lack a visible Back affordance are a documented discoverability failure.

## Options

Each option is rated on: **conflict** (with single-finger move + edge rockers), **discoverability**,
and **speed**.

### Option A — Two-finger tap = Back  *(recommended primary)*
A two-finger tap anywhere on the pad sends Back. Lives in the multi-finger space, so **zero conflict**
with single-finger move/tap or the edge rockers. Fast once known.
- Conflict: none · Discoverability: low alone · Speed: high

### Option B — Two-finger swipe (← horizontal) = Back
The macOS convention. Familiar to laptop users; still multi-finger so no single-finger conflict.
- Conflict: none (multi-finger) · Discoverability: low–medium · Speed: high · Risk: more accidental
  fires than a deliberate two-finger *tap*

### Option C — Persistent visible Back affordance  *(recommended companion)*
A small, always-visible Back control — a chip in a top bar, or a labelled button by the grip — so Back
is never hidden. Directly answers the Siri-Remote failure mode.
- Conflict: none · Discoverability: high · Speed: high · Cost: a little chrome on the calm surface

### Option D — Top-edge swipe-in = Back
The left/right edges are taken, but the **top** edge is free; an inward swipe from the top = Back
(echoes Android edge-back without colliding with vol/channel).
- Conflict: low (top edge unused) · Discoverability: low · Speed: medium · Risk: competes with the
  status bar / now-playing area near the top

### Always-on fallback — Back in the command sheet
Regardless of the gesture chosen, the pull-up command sheet keeps an explicit **Back** action, so Back
is always reachable even if a user never learns the gesture.

## Recommendation

**A + C together**, taught by the first-run card: a **two-finger tap** for speed *and* a **persistent
visible Back affordance** for discoverability — never a hidden gesture alone (the Siri-Remote lesson).
Keep the command-sheet Back as the universal fallback. (B is a fine optional addition for laptop-trained
users; D is the weakest given the busy top area.)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Back without leaving the pad (Priority: P1)
From the gesture pad I can go Back with a quick gesture (two-finger tap) and, if I don't know it, with a
visible Back control — without it ever interfering with moving the pointer or changing volume/channel.

**Acceptance Scenarios**:
1. **Given** the pad, **When** I two-finger tap, **Then** Back is sent and the pointer does **not** move.
2. **Given** I single-finger drag, **When** I move, **Then** the pointer moves and **no** Back fires.
3. **Given** I use an edge rocker, **When** I swipe it, **Then** volume/channel changes and **no** Back fires.
4. **Given** I never learned the gesture, **When** I look at the screen, **Then** a visible Back control is present (and/or in the command sheet).

### Edge Cases
- Two-finger tap mis-detected as a pinch/zoom → must be distinguished from a two-finger drag/pinch.
- A user who lifts one finger mid-two-finger gesture → must not fire pointer-move spuriously.
- Reduce-motion / accessibility → the visible Back affordance must carry a `contentDescription` and a ≥48dp target.

## Requirements *(mandatory)*

- **FR-001**: Back MUST be triggerable from the gesture pad by an input that does **not** conflict with
  single-finger pointer-move or the edge volume/channel rockers (recommended: two-finger tap).
- **FR-002**: A **visible** Back affordance MUST exist (persistent control and/or the command-sheet Back);
  Back MUST NOT depend on a hidden gesture alone.
- **FR-003**: Any Back gesture MUST be taught by the first-run card (alongside the edge gestures).
- **FR-004**: The Back gesture MUST be reliably distinguished from pinch/scroll/two-finger-drag.

## Success Criteria *(mandatory)*

- **SC-001**: A user can trigger Back from the pad within the first session without external help (visible affordance + teaching).
- **SC-002**: Across normal use, the Back input never mis-fires during pointer-move or volume/channel changes (0 false positives in manual testing).
- **SC-003**: The chosen gesture is sent reliably (≥95% recognised in manual trials).

## Assumptions

- Back is wired (`onNav(NavButton.BACK)`); this spec is about the **input affordance**, not the command.
- Multi-finger detection is feasible via Compose pointer input (pointer count).
- Final pick (A+C vs alternatives) is the user's call; A+C is the recommendation.
