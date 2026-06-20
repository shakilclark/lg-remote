# Feature Specification: Activate Inactive UI Shells (feature-wiring backlog)

**Feature Branch**: `020-activate-shells`

**Created**: 2026-06-20

**Status**: Draft — coordination/backlog spec

**Relates to**: the `010` faithful UI rebuild (which renders every surface from the designs), and the
feature specs `006` (audio output), `009` (notification), `016` (expanded media controls). Adds the
items that don't yet have a home spec.

**Input**: "Add all layout, leaving yet-to-implement features inactive; add a spec to plug in
functionality for them."

## Why this feature (context)

The faithful UI rebuild builds **every** surface from the design artifacts, but several controls map to
TV features that aren't implemented yet. Rather than omit them (and reflow the layout later) or fake
them, the rebuild renders them as **clearly-inactive placeholder shells**. This spec is the backlog to
**plug in** each one — so "inactive" is a tracked, temporary state with a known activation path, not a
dead end. The layout never changes when a shell goes live; only its behaviour does.

## Inactive-shell inventory

| Shell (UI present, inactive) | What it should do | Mechanism | Owning spec | Priority |
|---|---|---|---|---|
| **Voice / mic** (top bar) | Voice search / dictation to the TV | webOS voice pipeline (non-trivial; or launch TV voice search) | **new** (021?) | P3 |
| **Power on** (top bar power; *power-off already works*) | Wake the TV when off | Wake-on-LAN (MAC + magic packet) | **new** | P2 |
| **Audio output / Sound** (command sheet) | Switch TV speakers / soundbar / BT / etc. | `ssap://audio/getSoundOutput` + `changeSoundOutput` | **006** | P2 |
| **Keypad numbers (0–9)** (command sheet) | Channel/PIN entry | button injection over the pointer socket | **016**-adjacent | P2 |
| **Colour keys (R/G/Y/B) + Guide** (command sheet) | Coloured-key + guide functions | button injection (RED/GREEN/YELLOW/BLUE/PROGRAM) | **016** | P3 |
| **Keyboard / Type** (command sheet) | On-screen text entry to the TV | `ssap://com.webos.service.ime/...` insertText/enter | **new** | P2 |
| **Now-playing artwork** (cover) | Real art when the source provides it | channel logo / `com.webos.service.mediacontroller` | **004/016** | P3 |
| **Lock-screen / shade notification** | Control while locked | MediaStyle notification + foreground service | **009/008** | P2 |
| **Expanded media controls (CC / skip / more)** (cover) | Captions, skip, info, etc. | button injection (CC/GOTOPREV/NEXT/INFO…) | **016** | P2 |

## Requirements *(mandatory)*

- **FR-001**: Every inactive shell MUST be **visually distinguishable as inactive** (disabled state /
  reduced emphasis / "coming soon" affordance) and MUST NOT pretend to work or show fabricated state.
- **FR-002**: Activating a shell MUST NOT change the layout — only wire behaviour — so the design stays
  stable as features land.
- **FR-003**: Each shell MUST map to an activation path (mechanism + owning spec) tracked in the table
  above; items without an existing spec (voice, power-on/WoL, keyboard/Type) get their own specs before
  implementation.
- **FR-004**: Tapping an inactive shell MUST give honest feedback (e.g. a "not available yet" message),
  never a silent no-op or a fake success.
- **FR-005**: As each feature spec lands, its row here MUST be checked off / removed, so this backlog
  shrinks to empty.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of placeholder controls are either active or unmistakably inactive — no control looks
  live but does nothing.
- **SC-002**: Every inactive shell has a documented activation path (mechanism + owning spec).
- **SC-003**: When a feature is wired, the surface's layout is unchanged from the rebuild (only
  behaviour differs).
- **SC-004**: The inventory reaches zero inactive shells once 006/009/016 + the new voice/WoL/keyboard
  specs are implemented.

## Assumptions

- Power-**off** is already wired (`system/turnOff`); only power-**on** (Wake-on-LAN) is outstanding.
- Keypad/colour/Guide/CC reuse the existing pointer-socket button-injection path (feasible now; gated
  here only to keep the rebuild layout-first).
- Voice, Wake-on-LAN, and on-screen keyboard need their own specs before implementation (flagged
  "new").
