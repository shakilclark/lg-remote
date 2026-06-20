# Feature Specification: Expressive Motion & Flourishes

**Feature Branch**: `010-expressive-redesign` (motion layer; authored alongside the redesign)

**Created**: 2026-06-20

**Status**: Draft

**Builds on / extends**: `010-expressive-redesign`. This is the **motion layer** of the redesign —
the transitions and micro-flourishes that make the M3 Expressive surfaces feel alive. It adds no new
screens; it specifies how the screens `010` defines move.

**Input**: "Research and add visual expressive flourishes and transitions — not overdoing it."

## Why this feature (context)

Motion is a headline of Material 3 Expressive: a spring-based system meant to make transitions feel
alive and physical. Done well it makes the remote feel premium; overdone it reads as gimmicky and, on
a control surface used in a hurry, *slows the user down*. This spec is deliberately **restraint-first**:
it picks a small set of high-value transitions and flourishes, ties each to a real navigational or
feedback purpose, and caps how much runs at once. Everything degrades cleanly under "reduce motion".

Reference (Material guidance): container transform for hero moments; shared-axis for forward/back
navigational relationships; spring-based `MotionScheme` for spatial vs effect motion. The codebase
already has `MotionSpecs` (springs) and `pressMorph` as the fallback vocabulary.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transitions that explain navigation (Priority: P1)

As I move between the home pad, the command sheet, its segments, and the now-playing cover, the motion
*tells me where things came from and go back to* — the sheet rises and settles, segments cross-fade
along a shared axis, the now-playing bar expands into the cover view, and Back reverses it.

**Why this priority**: Transitions earn their place only when they aid orientation; this is the core,
purposeful use of motion.

**Acceptance Scenarios**:

1. **Given** the home surface, **When** I pull the grip, **Then** the command sheet rises with one
   spring settle (no bounce-y overshoot loop) and Back/swipe-down reverses it.
2. **Given** the sheet open, **When** I switch segments, **Then** content transitions on a shared axis
   (a quick slide+fade), not a hard cut.
3. **Given** a now-playing bar, **When** I tap it, **Then** it expands into the cover view as one
   continuous container transform, and ✕/Back collapses it back to the bar.

### User Story 2 - Tactile press flourishes (Priority: P2)

Pressing a control answers instantly and physically: it shape-morphs + springs slightly, the icon's
fill animates in when it becomes active/selected, and the recessed pad gives subtle press feedback.

**Acceptance Scenarios**:

1. **Given** any control, **When** I press it, **Then** it morphs rounder + scales ~0.94 on a snappy
   spring and settles on release, with a haptic on press-down.
2. **Given** a selectable control (active sheet segment, playing state), **When** it becomes selected,
   **Then** its Material Symbol transitions from outlined to **filled**.

### User Story 3 - State motion that informs, never decorates (Priority: P2)

Connection and loading states use motion to communicate: a soft pulse on the live/connected dot, the
expressive **loading indicator** while scanning/pairing, and a gentle cross-fade when the connection
chip changes state.

**Acceptance Scenarios**:

1. **Given** scanning or pairing, **When** I wait, **Then** an expressive loading indicator shows
   progress is happening (no static "frozen" UI).
2. **Given** the connection state changes, **When** it flips (e.g. Resuming… → Connected), **Then** the
   chip cross-fades rather than snapping.

### User Story 4 - Restraint & accessibility (Priority: P1, cross-cutting)

The motion never overwhelms: at most one "hero" transition runs per surface at a time, intensity is
capped, and with "reduce motion" on, every flourish degrades to a plain state change with no loss of
information or function.

**Acceptance Scenarios**:

1. **Given** any single screen, **When** it animates, **Then** no more than one orchestrated hero
   motion plays at once (the rest are quick, subordinate micro-feedback).
2. **Given** system "reduce motion" is on, **When** I use the app, **Then** shape-morph/pulse/container
   transforms are replaced by instant or simple cross-fades, and nothing is conveyed by motion alone.

### Edge Cases

- Rapid repeated input (mashing volume, fast segment switches) → motion must not queue/lag behind
  input; interrupts cleanly (springs retarget, no animation backlog).
- Low-end device / dropped frames → motion must degrade gracefully and never block the control action
  (actions fire immediately; motion is cosmetic).
- A transition mid-disconnect → the container transform still completes or cancels cleanly; it never
  traps the user.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Screen/sheet transitions MUST use spring-based motion (`MotionScheme`/`MotionSpecs`): the
  command sheet rises/settles on a spatial spring; segment switches use a shared-axis slide+fade.
- **FR-002**: The now-playing bar → cover view MUST be a single **container transform** (the one hero
  motion of that surface), reversible via ✕/Back.
- **FR-003**: Press feedback MUST be shape-morph + spring scale + haptic (the existing `pressMorph`),
  with ripple off where the morph is the feedback.
- **FR-004**: Selected/active controls MUST animate the Material Symbols **Fill** axis
  (outlined→filled) on selection.
- **FR-005**: Indeterminate waits (scanning, pairing, reconnecting) MUST show an expressive loading
  indicator; connection-chip state changes MUST cross-fade.
- **FR-006**: At most one orchestrated "hero" transition MUST run per surface at a time; ambient/
  decorative looping animation beyond the single connection pulse is prohibited.
- **FR-007**: All motion MUST respect the system "reduce motion" setting, degrading to instant/simple
  cross-fades; no information may be conveyed by motion alone.
- **FR-008**: Motion MUST never gate a control action — the SSAP command fires immediately; animation
  is cosmetic and interruptible.
- **FR-009**: Predictive-back, where the platform supports it, SHOULD drive the reverse of the
  sheet/cover transitions so the back gesture feels connected.

### Key Entities

- **MotionScheme/MotionSpecs**: the spring vocabulary (spatial vs effects) every transition draws from.
- **Transition role**: container-transform (hero), shared-axis (nav), fade/cross-fade (state) — each
  flourish is tagged with one and only one role.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every animated element maps to exactly one purpose (navigation, feedback, or state) — no
  motion exists purely for decoration (auditable against this spec's role list).
- **SC-002**: With "reduce motion" on, 100% of flourishes degrade to instant/simple changes and the app
  remains fully usable and informative.
- **SC-003**: A control action's effect is never delayed by its animation — the command is sent on
  press, measured independent of motion.
- **SC-004**: No screen plays more than one hero transition simultaneously (verifiable by inspection of
  each surface).
- **SC-005**: Motion holds ~60fps on the target device and interrupts cleanly under rapid input (no
  visible animation backlog).

## Assumptions

- The Compose dependency carries the Expressive `MotionScheme`; where unavailable, `MotionSpecs`
  springs and `pressMorph` provide the same feel (per `010` §0/§6).
- Notification/lock-screen motion is owned by the system MediaStyle surface and is out of scope here.
- "Reduce motion" maps to the platform animator/transition-scale + accessibility setting the app can
  observe.
