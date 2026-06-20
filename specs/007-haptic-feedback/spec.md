# Feature Specification: App-Wide Haptic Feedback

**Feature Branch**: `007-haptic-feedback`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the native Android remote) and the `docs/design-system.md`
motion/feedback principle that "the TV is across the room, so the *phone* must confirm the action."

**Input**: User description: "Haptic feedback — make the remote feel physical so every actionable
touch confirms itself. Replace the current ad-hoc LongPress-everywhere with a coherent, app-wide
haptic design: a small vocabulary of feels mapped to interaction types, respecting system settings,
centralised in one place."

## Why this feature (context)

A TV remote is operated by feel as much as by sight — the eyes are on the TV, not the phone. The
design system already names this: principle "Tactile feedback over chrome" says *every press answers
instantly — shape-morph + spring scale + haptic* (§1.2), and §9 Accessibility requires respecting
*Reduce motion*. But today the implementation is **ad-hoc**: every interactive component independently
calls `haptics.performHapticFeedback(HapticFeedbackType.LongPress)` on tap — D-pad keys, OK, the
bottom bar, app-shortcut tiles, the input-switcher rows, the touchpad (drag-start *and* tap), and
every transport/volume key via the shared `ControlKey`.

Two problems follow:

1. **Everything feels the same.** A light directional nudge, a confirming "launch Netflix", muting,
   and a rejected command (sent while disconnected) all produce the identical `LongPress` buzz —
   the strongest stock effect, fired for the lightest taps. There is no vocabulary, so the haptics
   carry no information.
2. **It is scattered and untestable.** The mapping of "interaction → effect" lives inline in eight
   call sites. There is no single place to change the feel, no fallback strategy for newer constants,
   no honouring of an in-app on/off preference, and nothing a test can assert against.

This feature defines a **small, coherent haptic vocabulary**, maps each remote interaction to one
entry in it, and routes everything through a **single centralised helper** so the feel is consistent,
tunable, and testable — replacing the eight inline `LongPress` calls.

### Current state (audit)

| Call site | File | Today |
|---|---|---|
| Workhorse key (D-pad arrows, OK, volume, channel, transport — all use `ControlKey`) | `ui/components/ControlKey.kt` | `LongPress` on press-down |
| D-pad | `ui/DPad.kt` | `LongPress` |
| App-shortcut tiles (YouTube / Netflix) | `ui/AppShortcuts.kt` | `LongPress` on tap |
| Input-switcher rows | `ui/InputSwitcher.kt` | `LongPress` on tap |
| Bottom action bar | `ui/BottomBar.kt` | `LongPress` on tap |
| Touchpad | `ui/TouchPad.kt` | `LongPress` on **drag-start** and on **tap** |

Consistent: every actionable touch *does* fire a haptic (good baseline). Inconsistent: it is always
the same maximal `LongPress`, regardless of whether the action is a light nudge, a confirmation, a
toggle, or a failure; mute on/off feel identical; touchpad-tap and touchpad-edge are
indistinguishable; and a command sent while disconnected still buzzes as if it succeeded.

## Modern Android haptics — design research *(informative; informs the requirements)*

Targets here are `minSdk 31` (Android 12) per the project constitution. Findings, with sources:

- **`View.performHapticFeedback(int)` / `HapticFeedbackConstants`** is the recommended, no-permission
  path for *semantic* UI haptics: you name the *meaning* (a confirm, a tick) and the OS plays the
  device-tuned effect. It needs **no `VIBRATE` permission** and degrades gracefully on devices that
  lack an effect. ([Android — Haptic feedback](https://developer.android.com/develop/ui/views/haptics/haptic-feedback),
  [Haptics API reference](https://developer.android.com/develop/ui/views/haptics/haptics-apis))
- **Constant availability by API level** (relevant to `minSdk 31`):
  - `CLOCK_TICK`, `KEYBOARD_TAP`, `VIRTUAL_KEY`, `LONG_PRESS`, `CONTEXT_CLICK` — long-established
    (≤ API 23); always available to us.
  - `CONFIRM`, `REJECT`, `GESTURE_START`, `GESTURE_END` — **added in API 30** (Android 11); available
    on every device we target. ([HapticFeedbackConstants](https://developer.android.com/reference/android/view/HapticFeedbackConstants))
  - `SEGMENT_TICK`, `SEGMENT_FREQUENT_TICK`, `TOGGLE_ON`, `TOGGLE_OFF` — **added in API 34**
    (Android 14); must be **guarded with a fallback** for our API 31–33 users.
- **Compose `androidx.compose.ui.hapticfeedback.HapticFeedbackType`** historically exposed only
  `LongPress` and `TextHandleMove`; recent Compose UI/Foundation (2025, ~1.8/1.9) **expanded it** to
  `Confirm`, `Reject`, `GestureEnd`, `GestureThresholdActivate`, `SegmentTick`, `SegmentFrequentTick`,
  `ToggleOn`, `ToggleOff`, `ContextClick`, `VirtualKey`, `KeyboardTap`, etc. — i.e. a thin wrapper
  over the `HapticFeedbackConstants` above.
  ([HapticFeedbackType](https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType),
  [composables.com](https://composables.com/docs/androidx.compose.ui/ui/classes/HapticFeedbackType))
  Using these means the helper can stay in pure Compose where the type exists; where a richer/older
  fallback is needed it can drop to `View.performHapticFeedback` or a `VibrationEffect`.
- **`VibratorManager` / `VibrationEffect`** (API 31 `getSystemService(VibratorManager.class)`) give
  *bespoke* effects: `VibrationEffect.createPredefined(EFFECT_CLICK / EFFECT_HEAVY_CLICK / EFFECT_TICK
  / EFFECT_DOUBLE_CLICK)` and composition primitives (`VibrationEffect.Composition` with
  `PRIMITIVE_CLICK`, `PRIMITIVE_TICK`, etc., availability queryable via
  `areAllPrimitivesSupported`). **This path requires the `VIBRATE` permission** and bypasses the
  user's *touch-feedback* system setting, so it MUST be used sparingly and only where a semantic
  constant cannot express the feel (candidate: the two-step "error / rejected command" buzz, and a
  distinct mute-toggle feel on API 31–33 where `TOGGLE_ON/OFF` is absent).
  ([Haptics API reference](https://developer.android.com/develop/ui/views/haptics/haptics-apis))

**Conclusion that shapes this spec**: prefer **semantic Compose `HapticFeedbackType`** (no
permission, OS-tuned, honours system settings) as the default for all interactions; reach for
`VibrationEffect` only for the handful of feels the semantic set cannot express on `minSdk 31`, and
keep every such decision inside one helper so it is auditable and testable.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Every actionable touch confirms itself (Priority: P1)

When I tap any control on the remote, the phone answers instantly with a haptic so I know the press
registered without looking down at the phone. Lighter, frequent actions (arrows) feel light; bigger
actions (launching an app, switching input) feel more deliberate.

**Why this priority**: This is the core of the feature and of the design system's "tactile feedback
over chrome" principle. Without a felt confirmation the remote is silent in the hand while the user's
eyes are on the TV.

**Independent Test**: With the app open, tap a D-pad arrow, then OK, then a YouTube shortcut, on a
device with vibration enabled — each produces a haptic, and the arrow's feel is perceptibly lighter
than OK's / the shortcut's confirm.

**Acceptance Scenarios**:

1. **Given** vibration is enabled on the device, **When** I tap a D-pad arrow, **Then** I feel a
   light tick.
2. **Given** vibration is enabled, **When** I tap OK / launch an app / switch input, **Then** I feel
   a slightly stronger "confirm" that is distinct from the arrow tick.
3. **Given** any actionable control, **When** I press it, **Then** the haptic fires on press-down
   (paired with the press-morph), not after the network round-trip — feedback is immediate and does
   not wait on the TV.

---

### User Story 2 - Different actions feel different (a vocabulary) (Priority: P1)

The remote uses a small set of distinct feels so I can tell — by touch alone — the difference between
nudging a direction, confirming a launch, toggling mute on vs off, and a command that was rejected.

**Why this priority**: Identical haptics on every action (today's `LongPress`-everywhere) carry no
information. A small vocabulary is what makes the remote feel "physical" rather than just "buzzy".

**Independent Test**: Trigger one action from each vocabulary category and confirm each is
distinguishable: arrow (tick), OK/launch (confirm), mute-on vs mute-off (two distinguishable
toggles), a command while disconnected (error).

**Acceptance Scenarios**:

1. **Given** the TV is unmuted, **When** I tap mute, **Then** I feel a "toggle-on" haptic; **When**
   I tap it again to unmute, **Then** I feel a distinguishable "toggle-off" haptic.
2. **Given** a connected TV, **When** I tap a navigation/transport key, **Then** I feel the light
   navigation tick (not the heavier confirm).
3. **Given** the touchpad, **When** I tap to click, **Then** I feel the confirm feel; **When** I
   start a drag, **Then** I feel a lighter gesture-start tick distinct from the click.

---

### User Story 3 - A failed or rejected command feels wrong (Priority: P2)

If I press a control when the remote can't actually reach the TV (disconnected / reconnecting /
off-network), the phone gives me a distinct "that didn't go through" haptic instead of the normal
success feel — so I'm not fooled into thinking the command landed.

**Why this priority**: Constitution principle IV ("fail visibly rather than silently") and `002`
edge case "a command sent while reconnecting … never silently dropped." A success-feeling buzz on a
dropped command actively misleads. High value, but it builds on the vocabulary (US1/US2) being in
place first.

**Independent Test**: Put the app in a disconnected state (TV off / airplane mode), tap a control,
and confirm the haptic is the distinct error feel — not the normal confirm/tick.

**Acceptance Scenarios**:

1. **Given** the connection state is disconnected/off-network, **When** I tap a control that sends a
   TV command, **Then** I feel the error/reject haptic and the command is not silently treated as
   successful.
2. **Given** the connection recovers, **When** I tap the same control, **Then** the normal
   success-feel returns.

---

### User Story 4 - Respect my settings (system + in-app) (Priority: P1)

If I've turned off touch vibration in Android settings (or enabled reduce-motion / accessibility
preferences that suppress it), the remote stays silent — it never forces vibration on me. I can also
turn the remote's own haptics off inside the app.

**Why this priority**: Honouring the system touch-feedback setting is the platform-correct,
respectful default and is implicit in using the semantic haptic path; the design system's §9 also
requires respecting *Reduce motion*. Forcing vibration via the raw vibrator against a user's setting
is a defect, so this is P1 alongside the core feel.

**Independent Test**: Disable system touch vibration, open the app, tap controls — no vibration
occurs anywhere; re-enable it and vibration returns. Toggle the in-app haptics switch off and confirm
all haptics stop regardless of the system setting.

**Acceptance Scenarios**:

1. **Given** the OS touch-feedback / haptics setting is off, **When** I tap any control, **Then** no
   haptic fires (the default semantic path naturally honours this).
2. **Given** an in-app "Haptic feedback" toggle exists and I turn it off, **When** I tap any control,
   **Then** no haptic fires anywhere in the app, and the choice persists across launches.
3. **Given** reduce-motion / the design-system's reduced-feedback mode is active, **When** I interact,
   **Then** non-essential haptics are suppressed or softened consistently with §9.

---

### Edge Cases

- **Device has no haptic actuator / weak vibrator**: semantic feedback simply produces nothing or a
  best-effort effect — the app must never crash or block on haptics, and must not log noisily.
- **API 31–33 device (no `TOGGLE_ON/OFF`, `SEGMENT_TICK`)**: the helper MUST fall back to an
  available constant (e.g. `CONFIRM` / `CLOCK_TICK`) or a `VibrationEffect` so toggle/tick feels
  still differ as much as the hardware allows, with no exception.
- **Rapid key repeat (hold-to-repeat volume/channel from design §6)**: haptics must not machine-gun
  uncomfortably — repeated ticks should be the light tick (or throttled), never the heavy confirm on
  every repeat.
- **Haptic fired but command later fails async**: the press-down success-tick is acceptable for
  immediacy (US1.3); a *subsequently* observed failure surfaces via the connection-state UI, and a
  command issued while *already known* disconnected gets the error feel up-front (US3).
- **In-app toggle off but system setting on (and vice-versa)**: the effective rule is AND — a haptic
  fires only if both the system allows it (for the semantic path) and the in-app toggle is on.
- **Accessibility services / TalkBack**: must not double-buzz or conflict; defer to the platform's
  own feedback where it already provides it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST define a small, named **haptic vocabulary** (a closed set of semantic
  feels, e.g. *navigation-tick, confirm, toggle-on, toggle-off, gesture-start, error*) and map every
  interactive control to exactly one entry, replacing the current `LongPress`-everywhere.
- **FR-002**: All haptic playback MUST be routed through a **single centralised helper / abstraction**
  (one place that maps a vocabulary entry → the concrete Android/Compose effect). No UI component may
  call `performHapticFeedback` or the vibrator directly; the eight existing inline call sites
  (`ControlKey`, `DPad`, `AppShortcuts`, `InputSwitcher`, `BottomBar`, `TouchPad` ×2) MUST be migrated
  to the helper.
- **FR-003**: The helper MUST prefer the **semantic, no-permission path** (Compose
  `HapticFeedbackType` / `View.performHapticFeedback` with `HapticFeedbackConstants`) as the default
  for every interaction, so the OS plays its device-tuned effect and the user's system touch-feedback
  setting is honoured automatically.
- **FR-004**: For vocabulary entries whose ideal constant is **not available at `minSdk 31`** (e.g.
  `TOGGLE_ON/OFF`, `SEGMENT_TICK` — API 34+), the helper MUST provide an explicit, documented
  **fallback** to an available constant or a `VibrationEffect`, so the feel still differs across
  API 31–33 as far as the hardware permits.
- **FR-005**: The app MUST honour the **system touch-feedback / haptics setting**: when the OS has
  haptic touch feedback disabled, the app MUST NOT vibrate. (Using the semantic path satisfies this;
  any `VibrationEffect` path MUST additionally check the relevant system setting before firing.)
- **FR-006**: The app MUST honour the **reduce-motion / reduced-feedback** preference consistently
  with design-system §9 — suppressing or softening non-essential haptics when it is active.
- **FR-007**: The app MUST provide an **in-app "Haptic feedback" on/off toggle**; when off, no haptic
  fires anywhere in the app. The setting MUST persist across launches in the app's private storage.
  The effective enable rule is the **logical AND** of (system allows) and (in-app toggle on).
- **FR-008**: Haptics MUST fire on **press-down** (paired with the press-morph), giving immediate
  feedback that does not wait on the TV round-trip.
- **FR-009**: When a control that sends a TV command is pressed while the connection state is
  **disconnected / reconnecting / off-network**, the app MUST play the **error** vocabulary entry
  (not a success feel), reinforcing constitution principle IV and `002`'s "never silently dropped".
- **FR-010**: Distinct interactions that today feel identical MUST become distinguishable per the
  mapping: **navigation/transport keys** → light tick; **OK / launch app / switch input** → confirm;
  **mute on vs mute off** → toggle-on vs toggle-off; **touchpad tap** → confirm vs **touchpad
  drag-start** → gesture-start tick.
- **FR-011**: Haptic playback MUST be **safe and non-blocking** — never crash, block the UI thread,
  or spam logs when the device lacks an actuator or an effect; absence of haptics MUST degrade
  silently.
- **FR-012**: Hold-to-repeat controls (volume/channel repeat, design §6) MUST NOT produce an
  uncomfortable stream of heavy haptics — repeats use the light tick and/or are throttled.
- **FR-013**: The vocabulary → effect mapping MUST be **unit-testable in isolation** from the UI
  (e.g. the helper is injectable/fakeable so a test can assert "interaction X requests vocabulary
  entry Y" without a real vibrator).

### Proposed interaction → haptic mapping *(the vocabulary)*

| Interaction | Vocabulary entry | Compose `HapticFeedbackType` (default) | `HapticFeedbackConstants` (View path) | Min API | Fallback for API 31–33 |
|---|---|---|---|---|---|
| D-pad arrow (up/down/left/right) | navigation-tick | `SegmentTick` (light) → else `ContextClick` | `SEGMENT_TICK` → `CLOCK_TICK` | 34 (tick) / always (clock) | `CLOCK_TICK` (always avail) |
| Back / Home / Channel ▲▼ | navigation-tick | same as arrow | `SEGMENT_TICK` → `CLOCK_TICK` | as above | `CLOCK_TICK` |
| Volume +/− (incl. hold-repeat) | navigation-tick (throttled on repeat) | `SegmentTick` | `SEGMENT_TICK` → `CLOCK_TICK` | as above | `CLOCK_TICK`, throttled |
| Transport ▶ ⏸ ⏮ ⏭ | navigation-tick | `SegmentTick` | `SEGMENT_TICK` → `CLOCK_TICK` | as above | `CLOCK_TICK` |
| **OK / select** | confirm | `Confirm` | `CONFIRM` | 30 (always for us) | — (native at 31) |
| **Launch app** (YouTube / Netflix tile) | confirm | `Confirm` | `CONFIRM` | 30 | — |
| **Switch input** (input-switcher row) | confirm | `Confirm` | `CONFIRM` | 30 | — |
| Bottom-bar action (open sheet / motion) | confirm | `Confirm` | `CONFIRM` | 30 | — |
| **Mute → on** | toggle-on | `ToggleOn` | `TOGGLE_ON` | 34 | `CONFIRM`, or `VibrationEffect.EFFECT_HEAVY_CLICK` |
| **Mute → off** | toggle-off | `ToggleOff` | `TOGGLE_OFF` | 34 | `CLOCK_TICK`, or `VibrationEffect.EFFECT_CLICK` (distinct from on) |
| Touchpad **tap (click)** | confirm | `Confirm` | `CONFIRM` | 30 | — |
| Touchpad **drag-start** | gesture-start | `GestureThresholdActivate` → else `LongPress` | `GESTURE_START` | 30 | `GESTURE_START` (native at 31) |
| **Command while disconnected** (any send-key) | error | `Reject` | `REJECT` | 30 | — (or two-pulse `VibrationEffect` for emphasis) |
| Long-press / context action (e.g. long-press OK → motion mode) | context | `LongPress` / `ContextClick` | `LONG_PRESS` / `CONTEXT_CLICK` | always | — |

Notes on the mapping:

- The two API-34 entries that matter on our `minSdk 31` are the **tick** (`SEGMENT_TICK`) and the
  **toggles** (`TOGGLE_ON/OFF`). For ticks, `CLOCK_TICK` is a perfectly good always-available
  fallback. For the **mute toggle**, the fallback MUST keep *on* and *off* perceptibly different
  (e.g. confirm-vs-tick, or two different predefined `VibrationEffect`s) since "did mute toggle?" by
  feel is the whole point.
- `CONFIRM`, `REJECT`, `GESTURE_START` are all API 30, so they are **native on every device we
  target** — no fallback needed; they are the workhorses of the vocabulary.
- A `VibrationEffect` path is the *only* one that bypasses the system touch-feedback setting, so it is
  reserved for the small number of fallbacks above and MUST gate on the system setting + in-app
  toggle itself (FR-005/FR-007).

### Key Entities *(include if feature involves data)*

- **Haptic vocabulary entry**: one of a small closed set of named feels (navigation-tick, confirm,
  toggle-on, toggle-off, gesture-start, error, context). Each maps deterministically to a concrete
  effect + fallback. Not persisted.
- **Haptic preference**: the user's in-app on/off choice for haptics, persisted in the app's private
  storage (the only new persisted state this feature adds).
- **Haptic capability/setting state**: the effective "may we vibrate now?" derived from the system
  touch-feedback setting, reduce-motion, the in-app toggle, and device actuator presence. Not
  persisted; evaluated at play time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of interactive controls route their haptic through the single centralised helper —
  zero direct `performHapticFeedback` / vibrator calls remain in `ui/` components (the eight current
  call sites are all migrated).
- **SC-002**: A user can distinguish, **by feel alone** on a haptics-capable device, at least the
  three core categories — navigation-tick vs confirm vs error — in informal testing.
- **SC-003**: Mute-on and mute-off produce perceptibly different haptics on both an API 34+ device
  and an API 31–33 device (verifying the fallback path).
- **SC-004**: With the OS touch-feedback setting **off**, no vibration occurs on any interaction
  (verified on device); with the in-app toggle **off**, likewise no vibration occurs.
- **SC-005**: A command pressed while disconnected produces the error haptic, not a success feel, in
  100% of attempts.
- **SC-006**: The vocabulary→effect mapping is covered by unit tests that assert the correct
  vocabulary entry is requested per interaction type and that the API-fallback selection is correct,
  without a real vibrator.
- **SC-007**: No measurable jank or main-thread block attributable to haptics during rapid
  hold-to-repeat (volume held), and no crash on a device/emulator with no actuator.

## Assumptions

- **minSdk 31** (per constitution / `002` clarification C1): `CONFIRM`/`REJECT`/`GESTURE_START` are
  native; only `SEGMENT_TICK`/`TOGGLE_ON`/`TOGGLE_OFF` (API 34) need fallbacks.
- **Compose-first**: the UI is pure Jetpack Compose; the helper is expected to sit over Compose's
  expanded `HapticFeedbackType` where available and drop to `View.performHapticFeedback` /
  `VibrationEffect` only where a constant or fallback requires it.
- **Semantic-first**: the default path is the no-permission semantic API; the `VIBRATE` permission is
  added **only if** a `VibrationEffect` fallback is actually used, and is justified at plan time.
- **Single source of feel**: design taste (which exact constant maps to "confirm" etc.) is owned by
  the helper + this spec; components express *intent* ("this is a confirm"), never raw constants.
- **No new screen**: the in-app haptics toggle lives in the existing settings surface; this feature
  does not introduce a settings screen of its own (it slots a single switch into whatever settings
  entry the app already has, or a minimal one if none exists yet).
- **English-only UI** for any new toggle label, consistent with `002`.

## Clarifications

### Session 2026-06-20

- **Constant strategy**: prefer semantic `HapticFeedbackType` / `HapticFeedbackConstants` (OS-tuned,
  no permission, honours system setting) over raw `VibrationEffect`; the latter is a fallback only.
- **API floor**: built for `minSdk 31`; `SEGMENT_TICK`/`TOGGLE_ON`/`TOGGLE_OFF` (API 34) get explicit
  fallbacks; `CONFIRM`/`REJECT`/`GESTURE_START` (API 30) are used directly.
- **Centralisation**: one helper owns the interaction→effect mapping; the eight inline `LongPress`
  call sites are migrated; the mapping is unit-testable in isolation.
- **Settings**: honour the OS touch-feedback setting and reduce-motion; add a persisted in-app
  on/off toggle; effective rule is the AND of system-allows and in-app-on.

### Open questions (resolve in /clarify or /plan)

- **OQ-1 — Compose version vs API guard**: do we bump the Compose BOM to the version that exposes the
  expanded `HapticFeedbackType` (Confirm/Reject/Toggle/SegmentTick) — coordinating with the design
  system's optional Expressive bump — or implement the helper over `View.performHapticFeedback` +
  `HapticFeedbackConstants` directly and skip the Compose-version dependency? (Affects FR-003 path.)
- **OQ-2 — Mute toggle fallback feel on API 31–33**: what exact pairing best conveys on-vs-off where
  `TOGGLE_ON/OFF` is absent — confirm-vs-tick, or two distinct predefined `VibrationEffect`s? Needs a
  quick device feel-test.
- **OQ-3 — Error haptic richness**: is the single `REJECT` constant emphatic enough for "command
  dropped", or do we want a two-pulse custom `VibrationEffect` (and thus accept the `VIBRATE`
  permission) for a clearly "wrong" feel?
- **OQ-4 — Reduce-motion scope**: which haptics count as "non-essential" and get suppressed under
  reduce-motion vs which (e.g. the error feel) are essential and always play?
- **OQ-5 — Settings home**: where does the in-app toggle live — is there an existing settings surface
  to host it, or does this feature stand up a minimal one?
- **OQ-6 — Hold-repeat cadence**: exact throttle for the volume/channel hold-repeat tick (every Nth
  repeat, or time-based) so it informs without buzzing continuously.
