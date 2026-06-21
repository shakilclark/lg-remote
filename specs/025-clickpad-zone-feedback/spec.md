# Feature Specification: Clickpad touch-zone highlight + larger OK zone

**Feature Branch**: `025-clickpad-zone-feedback`

**Created**: 2026-06-21

**Status**: Draft (research captured; implement later)

**Relates to**: `022-clickpad` (the `GesturePad` zones — Up/Down/Left/Right edges, centre OK, four corner
actions; `zoneAt()` hit-testing; the faint-at-rest / brighten-on-touch hints), `010-expressive-redesign`
(M3 surface + motion), `012-expressive-motion` (reduce-motion).

**Input**: User description: "highlight touch zones on clickpad. make ok touch zone larger. research."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The zone under my finger lights up (Priority: P1)

When the user touches the clickpad for a **tap** (not a glide), the **specific zone** their finger is in
— a direction edge (Up/Down/Left/Right), the centre **OK**, or a **corner** (Settings/Mute/Back/Home) —
**highlights** while the finger is down, so it's unambiguous which action will fire on release. The
highlight clears on release (or when the gesture becomes a cursor glide).

**Why this priority**: The pad packs nine actions into one surface; today the at-rest hints only brighten
*as a group* on touch — they don't tell you *which* zone you're actually in. Per-zone highlight closes
that gap and is the headline of this spec.

**Independent Test**: Touch each region in turn → only that region's highlight appears; release → it
clears. Start a glide → no zone highlight (cursor mode).

**Acceptance Scenarios**:

1. **Given** the pad at rest, **When** the user presses and holds a direction edge, **Then** that edge's
   zone highlights (and no other), and **When** released, **Then** the highlight clears.
2. **Given** a press in the centre, **When** held, **Then** the OK zone highlights; releasing fires OK.
3. **Given** a press in a corner, **When** held, **Then** that corner's zone highlights; releasing fires
   that corner action.
4. **Given** the press turns into a drag past touch-slop, **When** cursor glide begins, **Then** any zone
   highlight is suppressed (the surface is now a trackpad, not a button).

---

### User Story 2 - A forgiving OK target (Priority: P1)

The centre **OK** is the most-used action and should be **easy to hit**. Its touch zone MUST cover at
least the full visible OK key plus a comfortable margin, so a tap anywhere on (or just around) the disc
reliably clicks OK rather than leaking into a direction.

**Why this priority**: The current OK hit-zone is **smaller than the visible OK button** along the
horizontal axis (see Research), so taps on the left/right of the disc fall through to Left/Right — a
real mis-fire the user reported. Fixing the target size is as important as the highlight.

**Independent Test**: Tap the extreme left, right, top, bottom edges of the visible OK disc → all
register as OK, none as a direction.

**Acceptance Scenarios**:

1. **Given** the OK disc, **When** the user taps anywhere within the visible disc, **Then** OK fires (not
   a direction).
2. **Given** a tap just outside the disc but within the OK margin, **When** released, **Then** OK fires.
3. **Given** the enlarged OK zone, **When** the user taps a clear direction/corner region, **Then** that
   region still fires (the OK growth MUST NOT swallow the directional/corner zones).

### Edge Cases

- **Glide vs tap**: highlight only applies to taps; once `awaitTouchSlopOrCancellation` resolves to a
  drag, suppress/clear the highlight (no zone box trailing the cursor).
- **Reduce-motion**: the highlight appears/clears with a minimal cross-fade or snap (no large motion),
  consistent with `012`/`LocalReduceMotion`.
- **Moving between zones while down**: if the finger slides between zones before slop (still a tap),
  the highlight SHOULD follow the current zone, and the zone fired on release is the release-position
  zone (confirm in `/plan`).
- **OK zone vs corners/edges**: enlarging OK must stay reconcilable with the corner hit-boxes
  (`CORNER_X/Y`) and the directional wedges so no zone is unreachable.
- **Accessibility**: highlight is a visual aid only; the existing custom accessibility actions
  (Up/Down/Left/Right/OK/corners) remain the non-visual path and MUST be unaffected.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: While a **tap** gesture is in progress, the clickpad MUST visually highlight the single
  zone the touch is currently in (one direction edge, OK, or one corner).
- **FR-002**: The highlight MUST clear on release and MUST be suppressed once the gesture becomes a
  cursor glide (post-touch-slop drag).
- **FR-003**: The highlight MUST use M3 state-layer conventions — a semi-transparent overlay in the
  zone's content colour at the pressed-state opacity — not an arbitrary colour/shape (see Research).
- **FR-004**: The OK touch zone MUST cover the entire visible OK key plus a margin, and MUST be measured
  in a way that is **not narrower than the button on any axis** (i.e. aspect-correct, not the current
  normalized-ellipse distance).
- **FR-005**: Enlarging the OK zone MUST NOT make any directional or corner zone unreachable; zone
  precedence and boundaries MUST remain well-defined.
- **FR-006**: Highlight reveal/clear MUST respect reduce-motion.
- **FR-007**: The change MUST preserve existing behaviour: tap-to-fire per zone, glide-to-move cursor,
  OK-as-its-own-button (no pad engagement), the mute-state corner icon, and all custom accessibility
  actions.

### Key Entities

- **Touch zone**: a region of the pad (`PadZone`: Up/Down/Left/Right/Ok/Settings/Mute/Back/Home) returned
  by `zoneAt()`; the unit that both *fires an action* and *receives a highlight*.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every zone, pressing it highlights that zone and only that zone (9/9 zones).
- **SC-002**: Taps anywhere on the visible OK disc register as OK in 100% of trials (no direction leak).
- **SC-003**: No directional or corner zone becomes unreachable after the OK enlargement (all 8
  perimeter zones still fire).
- **SC-004**: With reduce-motion on, the highlight uses a reduced/instant transition (no spring/scale).

## Research notes *(input for `/plan`)*

**M3 state layers (highlight).** A state layer is a semi-transparent overlay in the *content* colour at a
fixed per-state opacity; only one applies at a time. For a press, M3 uses the **pressed** opacity
(~0.10) and a ripple from the contact point. For the pad, the zone highlight should be an `onSurface`
(or `onSurfaceVariant`) overlay at ~0.10–0.12, shaped to the zone (rounded-rect for corners, a wedge or
rounded-rect band for edges, a circle for OK), drawn under the existing faint glyph hints. This keeps it
token-driven and theme/dynamic-colour correct. ([M3 state layers](https://m3.material.io/foundations/interaction/states/state-layers))

**OK target size — the actual bug.** `zoneAt()` computes `hypot(nx-0.5, ny-0.5) < OK_RADIUS` where
`nx,ny` are normalized to width/height **independently**, so the OK zone is an **ellipse** scaled to the
pad's aspect ratio. On a tall pad the horizontal half-width of the OK zone (`OK_RADIUS × width`) is far
smaller than the vertical half — and smaller than the visible 84dp disc — so left/right taps on the disc
leak into Left/Right. **Recommendation**: hit-test OK in real dp against an aspect-correct radius
(e.g. distance in px, radius ≈ half the OK key + ~8dp margin ≈ 50dp), instead of normalized-ellipse
distance. Tune so the OK zone comfortably exceeds the visible disc without crowding the edges/corners.
M3's 48dp minimum-target guidance reinforces a generous central target.

**Highlight shape options** (decide in `/plan`): (a) simple — tint the whole quadrant/wedge the zone
covers; (b) refined — a contained rounded-rect/`pill` behind corners + a circular layer under OK +
directional bands for edges. (b) reads cleaner but is more drawing work.

## Assumptions

- Builds on `022`'s `GesturePad` (`zoneAt()`, the single `awaitEachGesture` tap/glide arbiter, the
  `HintOverlay`); this adds a per-zone highlight layer and re-shapes the OK hit-test.
- The flat M3 tonal surface (post-`022` redesign) stays; highlight is a state-layer overlay on it, not a
  return to skeuomorphic treatments.
- Pure client-side visual + hit-testing change; no SSAP/protocol impact.
