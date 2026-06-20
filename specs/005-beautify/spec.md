# Feature Specification: Beautify — Visual Polish Pass

**Feature Branch**: `005-beautify`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the live app) and `docs/design-system.md` (the
Material 3 Expressive design system). This is a **polish/beautify** spec, not a rewrite: the
layout is settled (full-width symmetrical 3×3 D-pad biased low, vertical volume strip bottom-
right, persistent bottom bar, bottom-sheet pad/apps/inputs, native TV settings, no persistent
banner when connected). The goal is to close the gap between what is on screen today and the
"premium remote" the design system describes.

**Input**: "Beautify — audit the current UI against the design system and modern Material 3
Expressive aesthetics, and specify concrete improvements (not a rewrite): spacing/rhythm,
visual hierarchy, the empty D-pad corners, bottom-bar density, color/elevation/state layers,
the touchpad surface, app-icon tiles, sheet styling, motion/press feedback consistency,
connect/reconnect screens, empty/loading/error states, iconography, accessibility, and the
overall premium feel."

## Why polish now (context)

The redesign settled the *layout* — what controls exist and where they live. But the
implementation drifted from `docs/design-system.md` in dozens of small ways: hand-rolled press
feedback that doesn't match `ControlKey`, hardcoded `.dp`/`.sp` literals instead of the `Space`
and type tokens, raw brand colors that turn screens into "logo soup", a touchpad and bottom bar
that ignore the shape/elevation system, and connect/reconnect screens still on emoji + ad-hoc
sizing. None of these are bugs; together they are the difference between "a working remote" and
"a remote that feels designed". This spec enumerates each gap as a checkable item so the polish
pass is bounded and verifiable.

## Goal

Every screen, control, and state reads as one coherent, premium Material 3 Expressive surface:
consistent press feedback, consistent spacing rhythm on the 4dp grid, a single restrained
accent, tonal (not shadow) elevation, and accessible targets/contrast — **without** changing
the settled layout, the control set, or any TV-control behaviour.

## Principles

These restate `docs/design-system.md §1` as the acceptance lens for this pass. Every item below
must serve at least one:

1. **One token, one source of truth.** No raw `.dp`/`.sp`/`Color(0x…)` literals in UI code where
   a `Space`, `MaterialTheme.typography`, `MaterialTheme.shapes`, or `colorScheme` token exists.
2. **One press, one feel.** Every tappable control uses the same press-feedback vocabulary
   (`pressMorph` + tonal shift + haptic, ripple off) so nothing feels "off-system".
3. **Dark-first, single accent.** Electric red appears only on OK, the live/connected state, and
   the active/pressed transport state. Neutral by default.
4. **Tonal elevation, not shadows.** Layers separate by `surfaceContainer*` role + 1dp hairline,
   never drop shadows (except the ≤2dp bottom-bar lift the design system allows).
5. **Physical motion.** Springs from `MotionSpecs`, never ad-hoc `tween`/linear; and the whole UI
   respects "reduce motion".
6. **Accessible by construction.** ≥48dp targets, AA contrast, never colour-only state, every
   icon-only control labelled.

## Audit — current issue → proposed improvement

Each item is independently checkable. Items reference the real component file and the design-
system section that governs it. Priority: **P1** = breaks the premium/consistency promise or
fails accessibility; **P2** = clear, visible polish; **P3** = refinement / nice-to-have.

### A. Tokens & consistency

- [ ] **A1 (P1) — Purge hardcoded literals.** `ConnectScreen.kt` (`46.sp`, `22.sp`, `20.dp`,
  `18.dp`, `14.dp`, `12.dp`, `10.dp`, `6.dp`, `8.dp`), `App.kt` (`18.dp`/`12.dp`/`14.dp`/`40.dp`),
  `InputSwitcher.kt` (`10.dp` ×2), `AppShortcuts.kt` (`26.sp`), and `RemoteScreen.kt`
  (`56.dp`, `440.dp`) use raw literals. Replace each with the nearest `Space.*` token and a
  `MaterialTheme.typography.*` style (design-system §3, §5.2). Where a value isn't on the 4dp
  scale (e.g. `18.dp`, `14.dp`, `6.dp`, `10.dp`, `11.dp`), snap it to the scale (`16`/`20`,
  `12`, `4`, `12`, keep `11.dp` dot as-is — it's a glyph size, not spacing).

- [ ] **A2 (P1) — Stop importing raw palette colors into screens.** `ConnectScreen.kt`,
  `InputSwitcher.kt`, and `ConnectionBanner.kt` import `Muted`/`Ok`/`Warn`/`AccentSoft` directly.
  Route everything through `MaterialTheme.colorScheme` roles (`onSurfaceVariant`, `tertiary`,
  `primary`, `error`) so the brand mapping in `Theme.kt` stays the single source of truth
  (design-system §2.2–§2.3). The status-dot colours in `ConnectionBanner` are the one justified
  exception (status semantics) — even so, prefer `tertiary`/`error`/`Warn`-via-role where a role
  exists.

- [ ] **A3 (P2) — `labelSmall` is undefined → below the 12sp floor.** `BottomBar.kt` captions use
  `typography.labelSmall`, which isn't defined in `Type.kt` and falls back to the M3 default
  (~11sp), violating the "never below 12sp" rule (design-system §3, §9). Either add a
  `labelSmall` (12sp) to `Type.kt` or switch the captions to `labelMedium` (12sp).

### B. D-pad cluster (`DPad.kt`, design-system §7.2)

- [ ] **B1 (P1) — Unify OK press feedback with `ControlKey`.** `OkCell` is hand-rolled: it scales
  (0.92) but does **not** corner-morph and has no tonal/overlay shift, so OK feels different from
  every arrow. Give OK the full vocabulary — a press morph (stronger: scale `0.92`, brief
  `primary` ring/overlay that springs out on release per §7.2) while keeping its persistent
  `primaryContainer` fill. Reuse `pressMorph` rather than a bespoke `graphicsLayer`.

- [ ] **B2 (P2) — OK glyph uses the wrong type role.** The "OK" text uses `titleLarge`; the design
  system reserves `headlineMedium` for the OK glyph / channel numbers (§3). Switch it.

- [ ] **B3 (P2) — Arrow glyphs are small and thin in large cells.** Arrows render at `32.dp` inside
  cells that are ~1/3 of a 328dp-wide row (~100dp+ squares), leaving the keys feeling empty and
  the chevrons looking weightless. Increase arrow icon size (≈`40dp`) and/or cap cell size so
  square keys land near the §5.3 `64dp` primary-key feel rather than ballooning; verify the
  cluster still reads as a tight `+` not four distant buttons.

- [ ] **B4 (P3) — The empty corners are dead void.** Four corner cells are bare `Spacer`s. Give the
  cluster a sense of being one object: render the four arrows + OK on a single `extraLarge`
  (36dp) `surfaceContainerLow` panel with a 1dp `outlineVariant` hairline (design-system §7.2,
  §8 ASCII), so the empty corners become intentional negative space inside a container rather
  than gaps in the background. (Keeps the layout — arrows/OK positions unchanged.)

### C. Volume strip (`Controls.kt`, design-system §7.3, §5.3)

- [ ] **C1 (P2) — Width is under the primary-key spec.** The strip is `56.dp` wide
  (`RemoteScreen.kt`); volume is a "primary key" and should read at the `64dp` target (§5.3).
  Widen to `64.dp` and confirm the right-thumb reach still works.

- [ ] **C2 (P2) — Mute state is icon-tint-only.** When muted, only the icon turns `error`; the
  design system calls for the *whole control to read muted* — desaturate/dim the pill and use a
  clear muted glyph (slash) so state isn't conveyed by a small colour change alone (§7.3, §9
  "don't rely on accent alone"). Keep the icon swap; add a fill/alpha shift.

- [ ] **C3 (P3) — No hold-to-repeat or repeat-pulse on Vol±.** §6/§7.3 specify hold-to-repeat with
  a subtle pulsing scale so the user sees the command firing. Add press-and-hold repeat to Vol+
  / Vol− with the §6 repeat pulse. *(Behavioural nuance, not a layout change — keep scope to the
  visual pulse + existing volume commands.)*

- [ ] **C4 (P3) — Pill shape.** The design system describes volume as vertical pill rockers
  (`RoundedCornerShape(50)`); current keys are square `ControlKey`s. Optionally morph the strip
  toward the pill silhouette for the "rocker" read, or accept the squared keys as a deliberate
  simplification — decide explicitly and note it.

### D. Bottom bar (`BottomBar.kt`, design-system §5.1, §7)

- [ ] **D1 (P1) — Items have no on-system press feedback.** `BarItem` is `clip + clickable` with
  the **default ripple** and no morph/tonal shift, so it feels different from every other control
  (which use `pressMorph`, ripple off). Give bar items the shared press vocabulary (subtle scale
  + tonal `surfaceContainerHigh` press tint, haptic, ripple off) per Principle "one press, one
  feel".

- [ ] **D2 (P2) — Caption type below the floor.** See A3 — bar captions must be ≥12sp.

- [ ] **D3 (P2) — The bar floats with no surface.** The bar sits directly on the canvas with no
  container; the design system describes a floating action bar with a ≤2dp lift (§5.1, §8).
  Seat the bar on a `surfaceContainer`/`surfaceContainerLow` band (optionally a `large` top-
  corner shape) with the allowed ≤2dp shadow, so it reads as a distinct dock and the seam
  between body and bar is intentional.

- [ ] **D4 (P3) — Density / Settings separation.** Six items across the width are tight on a 360dp
  phone; the Settings gap is only `Space.l`. Confirm each item still meets the 48dp target after
  D1, and consider a hairline divider (not just a gap) before Settings to make the "rare/
  disruptive" grouping legible (§7 intent).

### E. Touchpad sheet (`TouchPad.kt`, design-system §4.1, §7)

- [ ] **E1 (P2) — Wrong shape token + fixed height.** Uses `shapes.large` (28dp); a large surface
  panel should use a larger radius (consider `extraLarge`/36dp to match the cluster) and a height
  that adapts to the sheet rather than a hardcoded `440.dp` (A1). Pick the radius deliberately
  and reference the token.

- [ ] **E2 (P2) — No active/touch state.** The pad gives a haptic on drag-start but no visual
  confirmation it's "live" — add a subtle pressed/active surface shift (e.g. fill →
  `surfaceContainerHighest` or a faint `primary` border) while a drag is in progress, so the user
  sees the pad has engaged (Principle "tactile feedback over chrome").

- [ ] **E3 (P3) — Tap and drag-start fire the same `LongPress` haptic.** Differentiate: a lighter
  haptic for tap/click vs the engage haptic on drag-start, so the two gestures feel distinct.

### F. App tiles (`AppShortcuts.kt`, design-system §7.5)

- [ ] **F1 (P1) — Brand-colour fills are "logo soup".** Tiles fill the whole square with raw
  `Color(0xFFFF0000)` / `Color(0xFFE50914)`. The design system is explicit: chip/tile fill stays
  **neutral**; brand colour appears **on the icon only** (§7.5). Rework tiles to neutral
  `surfaceContainerHigh` with the brand mark/glyph carrying the colour, so the apps sheet doesn't
  fight the restrained palette.

- [ ] **F2 (P2) — Hand-rolled tile has no on-system press feedback.** `AppTile` is `clip +
  clickable` (default ripple). Build it on `ControlKey` (or apply `pressMorph` + ripple-off +
  tonal shift) so it presses like everything else.

- [ ] **F3 (P3) — Hardcoded mark sizing.** `fontSize = 26.sp` / `FontWeight.Black` should map to a
  type token (e.g. `headlineMedium` or `titleLarge`) (A1). Real brand glyphs/wordmarks would beat
  single-letter marks if assets are available — note as optional.

### G. Inputs sheet (`InputSwitcher.kt`, design-system §7.7)

- [ ] **G1 (P2) — Default `AssistChip` is off-system.** Stock `AssistChip` brings Material default
  shape, ripple, and outline that don't match the app's press vocabulary or shape scale. Either
  theme the chips to the app's tokens or, per §7.7, switch to full-width rows with a leading icon
  and a trailing `primary` check on the active input (the design system's stated input pattern),
  using the control-key press tint.

- [ ] **G2 (P3) — Header style + spacing.** "INPUTS" all-caps via direct `Muted` + ad-hoc `10.dp`
  spacing; align the header style with the Apps sheet ("Apps" uses `titleMedium`) for cross-sheet
  consistency, and use `Space.*` (A1, A2).

### H. Sheets (cross-cutting, `RemoteScreen.kt`, design-system §7.6–§7.7, §8)

- [ ] **H1 (P2) — Inconsistent sheet headers & padding.** The Apps sheet has a `titleMedium`
  "Apps" header; Inputs has a `Muted` "INPUTS"; the Pad sheet has no header. Standardise: every
  sheet gets the same header treatment (title + the design system's `large` top-corner shape) and
  the same content padding token, so the three sheets feel like one family.

- [ ] **H2 (P3) — Drag-handle / top-corner shape.** Confirm `ModalBottomSheet` uses the `large`
  (28dp) top corners and a consistent drag handle across all three sheets (§7.7, §8).

### I. Connect / reconnect / states (`ConnectScreen.kt`, `App.kt`, `ConnectionBanner.kt`)

- [ ] **I1 (P1) — Connect/reconnect screens are off-system.** `ConnectScreen` and `ReconnectView`
  use a 📺 emoji, hardcoded font sizes, raw `Muted`, and stock `Button`/`OutlinedButton` with
  default styling. Bring them onto the system: replace the emoji with a themed `Tv` icon in an
  accented/neutral container, use `displaySmall`/`titleLarge`/`bodyLarge` type roles, `Space.*`
  spacing, and the app's button/press treatment (design-system §3, §5, §7.6). This is the first
  screen a user sees — it must set the premium tone.

- [ ] **I2 (P2) — Loading state is a bare spinner.** Scanning/connecting use a plain
  `CircularProgressIndicator`. Tint consistently to the brand and, where the BOM allows, prefer
  the Expressive `LoadingIndicator` (design-system §7.6 upgrade note); otherwise a brand-tinted
  spinner with a clear label. Ensure the "Pairing…" state pairs `Warn` colour **with** text/icon
  (not colour alone).

- [ ] **I3 (P2) — Reconnect view's two buttons compete.** `ReconnectView` shows "Try now" and
  "Choose a different TV" as two equal-weight filled `Button`s, so there's no primary action.
  Make "Try now" the primary (filled) and "Choose a different TV" secondary (text/outlined), and
  apply system spacing/type.

- [ ] **I4 (P2) — Empty states.** Apps sheet has no empty state; Inputs shows a bare "No connected
  inputs"; a TV with no installed shortcut apps shows nothing. Give each sheet a calm,
  consistent empty state (icon + one line in `onSurfaceVariant`), matching the connect-screen
  tone.

- [ ] **I5 (P3) — Off-network / permission states.** `ConnectionBanner` covers `OffNetwork` and
  `PermissionRequired` with accent dots + text; verify their copy and iconography match the
  connect-screen treatment so every "can't control the TV" state looks intentional and on-brand.

### J. Motion & accessibility (cross-cutting, design-system §6, §9)

- [ ] **J1 (P1) — No "reduce motion" support anywhere.** The design system requires that when the
  OS "remove animations / reduce motion" setting is on, the press morph drops to a simple
  alpha/indication and infinite pulses (the `ConnectionBanner` breathing dot, any repeat pulse)
  are disabled (§9). Add a single reduce-motion check (e.g. a `LocalReduceMotion`/accessibility
  query) and honour it in `pressMorph`, `OkCell`, `ConnectionBanner`, and the C3 repeat pulse.

- [ ] **J2 (P1) — Verify minimum touch targets.** After B3/C1/D1/D4, confirm every tappable hits
  ≥48dp (§5.3, §9): bottom-bar items, volume keys, app tiles (currently `56.dp` — OK), input
  rows/chips. Add `minimumInteractiveComponentSize` / size floors where a control could shrink on
  small screens.

- [ ] **J3 (P2) — Content descriptions audit.** Most icon-only controls are labelled, but confirm
  full coverage: OK ("OK/Select"), each bottom-bar item (present), volume keys (present), app
  tiles (present), touchpad (present), and the status dot's state is exposed to TalkBack as text,
  not just colour (§9).

- [ ] **J4 (P2) — Contrast spot-check.** Verify `onSurfaceVariant`/`Muted` (`#8B91A1`) is used only
  for secondary text and never as a primary button label (§9), and that bottom-bar captions in
  `onSurfaceVariant` on the canvas/dock pass AA at their final size.

- [ ] **J5 (P3) — System bars / insets.** Root padding in `App.kt` is hardcoded and only
  `ReconnectView` applies `systemBarsPadding`. Apply consistent insets at the root so content and
  the bottom bar respect status/navigation bars and gesture areas on all screens.

## Success Criteria

### Measurable / verifiable outcomes

- **SC-001**: A grep of `ui/` finds **no** raw `.sp` literals and no `.dp` literals that bypass
  `Space.*` for spacing (icon/glyph sizes and intentional one-offs excepted and commented).
  (A1, F3)
- **SC-002**: Every tappable control in the connected remote and the three sheets uses the shared
  press vocabulary (`pressMorph`/`ControlKey`, ripple off, haptic) — no control still relies on
  the default ripple. (B1, D1, F2, G1)
- **SC-003**: No screen renders text below 12sp; the OK glyph uses `headlineMedium`. (A3, B2, D2)
- **SC-004**: The apps sheet contains no full-bleed raw brand-colour fills; brand colour appears
  on glyphs only. (F1)
- **SC-005**: With the OS "reduce motion" setting on, no infinite animation runs and presses use a
  non-spring fallback. (J1)
- **SC-006**: All interactive targets measure ≥48dp in the Layout Inspector; primary keys (D-pad
  arrows, OK, volume) read at the §5.3 sizes. (B3, C1, J2)
- **SC-007**: Connect, reconnect, and all empty/loading/error states use system type roles,
  `Space.*` spacing, themed icons (no emoji), and a single clear primary action where one
  applies. (I1–I4)
- **SC-008**: Side-by-side with `docs/design-system.md §7–§8`, each component matches its recipe
  or carries a written, deliberate deviation note (e.g. C4 squared volume keys).
- **SC-009**: All `@Preview`s still render and the app builds; visual regression confirmed on a
  real phone against a real TV (constitution Principle V) with **no** change to control behaviour
  or layout structure.

## Out of scope

This is a polish pass. The following are explicitly **not** part of `005-beautify`:

- **No layout/structure changes.** The 3×3 D-pad (with empty corners), the bottom-right volume
  strip, the persistent bottom bar and its item order, the pad/apps/inputs bottom sheets, and
  "no banner when connected" are settled and stay. Treatments inside a control (panel behind the
  D-pad, dock behind the bottom bar) are visual, not structural, and are in scope.
- **No control-set changes.** No new buttons, no removed buttons, no new screens. (The volume
  hold-to-repeat in C3 reuses the existing volume commands and is the one allowed behavioural
  nuance, purely to make firing visible.)
- **No TV-protocol / connection-logic changes.** SSAP, pairing, reconnection, discovery, and the
  pointer socket are untouched (002 owns those).
- **No new colour identity.** The electric-red dark brand and the §2.2 role mapping are fixed;
  this pass *uses* them correctly, it does not redesign them. Dynamic/Material-You colour stays
  off (§2.2).
- **No dependency/BOM bump required.** Every item is achievable on the current Compose BOM
  (1.3.x). Expressive components (`LoadingIndicator`, shape-morph buttons, `MaterialMotion`) are
  noted as optional *upgrades* (I2, B1) and may be deferred to a later slice.
- **No localization, no light theme, no tablet/landscape layout** — out of scope per 002.
- **No iconography redesign / custom icon set.** Swapping an emoji for a Material icon (I1) is in
  scope; commissioning a bespoke icon family is not.

## Assumptions

- The design system `docs/design-system.md` is the authority; where it and the current code
  disagree, the code is what changes (this spec's whole premise).
- Work stays on the current Compose BOM; any Expressive-component upgrade is an explicit, separate
  decision and not a prerequisite for "done".
- Verification is visual + on-device per the constitution; there are no automated visual-
  regression tests to satisfy, so previews + a real-device pass are the bar.
- "Reduce motion" is read from the platform accessibility setting; if the chosen Compose version
  lacks a direct API, a documented best-effort query is acceptable (J1).
