# LG Remote — Design System (Material 3 Expressive · Material You)

Source of truth for the **`010-expressive-redesign`** ("Direction C") UI. Replaces the previous
electric-red, dark-only, D-pad + bottom-bar spec. The look is **Material 3 Expressive** themed by
**Material You** (light **and** dark), with a fixed **Ultraviolet `#7B2FF7`** fallback seed.

> Visual reference: the rendered design studies live in [`docs/design/`](./design/) — open them in a
> browser and toggle light/dark.
>
> **Fidelity stance (decided 2026-06-20): these artifacts define the _direction_ — the structure,
> the component set, and the Ultraviolet _fallback_ palette — not pixel-truth.** The shipped app keeps
> the Direction-C structure but takes a **native finish**: Material You dynamic colour (so the
> artifacts' Ultraviolet only appears when dynamic colour is off / pre-Android-12) and standard
> Material 3 components reused/evolved rather than rebuilt to the mockups. The on-device look therefore
> legitimately differs from the mockups, especially in colour. This doc is the token/spec layer.
> Requirements: [`specs/010-expressive-redesign/spec.md`](../specs/010-expressive-redesign/spec.md).

---

## 0. Dependencies — enabling M3 Expressive + Material You

Bump the Compose BOM to a release carrying **stable Material 3 Expressive** (`material3` 1.4/1.5):

```toml
# gradle/libs.versions.toml — use the latest stable BOM that ships Expressive
composeBom = "2026.xx.xx"   # pulls material3 with MaterialExpressiveTheme, MotionScheme, MaterialShapes
```

This unlocks `MaterialExpressiveTheme`, `MotionScheme`, `MaterialShapes` (shape-morph), expressive
buttons/loading indicators, and `dynamicDarkColorScheme()/dynamicLightColorScheme()`. Expressive APIs
may still be `@ExperimentalMaterial3ExpressiveApi` — opt in per file. The codebase's existing
`pressMorph` is the graceful fallback wherever an Expressive component isn't used yet.

Icons: adopt **Material Symbols Rounded** (variable font / generated assets). `Icons.Rounded.*` from the
already-present `material-icons-extended` is the interim path; the **Fill** axis is used to mark
selected/active state.

---

## 1. Design principles

1. **Thumb-first, one calm surface.** The home screen *is* the remote: a single gesture pad. Glide to
   point, tap to click; right edge = volume, left edge = channel; swipe-in = Back. No button grid.
2. **Hidden, never lost.** Everything else (numbers, apps, inputs, sound, keyboard) lives in a command
   sheet you reach by **pulling a visible grip** — discoverable by sight, not by memorising gestures.
3. **Native to the phone.** Follow the system light/dark; take colour from the wallpaper (Material You)
   on Android 12+, Ultraviolet fallback otherwise. Strong contrast in both modes (WCAG AA).
4. **Tactile & expressive.** Spring motion, shape-morph on press, haptics. The TV is across the room,
   so the *phone* confirms every action.
5. **Honest to SSAP.** Show only what the TV actually reports — app icon + name + play-state. No
   invented album art, titles, scrubber, or next/previous. Real art only when the source gives it.
6. **Tonal, not heavy.** Depth comes from tonal surface containers, not skeuomorphism or stacked drop
   shadows.

---

## 2. Color — Material You + Ultraviolet fallback

### 2.1 Scheme resolution (replace `Theme.kt`)

```kotlin
@Composable
fun LGRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),   // follow the system by default (manual override in settings)
    dynamic: Boolean = true,                        // Material You where available
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        darkTheme -> UltravioletDark
        else      -> UltravioletLight
    }
    MaterialExpressiveTheme(           // expressive theme = expressive MotionScheme + shapes
        colorScheme = scheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}
```

Dynamic colour is **on** (this is a reset of the old brand-locked decision). The Ultraviolet schemes
below are the fallback identity. **Generate the exact tonal values from seed `#7B2FF7` with
`material-color-utilities`**; the tables below are hand-tuned representatives matching the design
artifacts — good enough to ship, but regenerate for canonical tones.

### 2.2 Ultraviolet — Dark (fallback)

| Role | Hex | | Role | Hex |
|---|---|---|---|---|
| `primary` | `#D7BBFF` | | `surface` | `#141218` |
| `onPrimary` | `#46177D` | | `surfaceContainerLow` | `#1C1922` |
| `primaryContainer` | `#5C2E9F` | | `surfaceContainer` | `#221E2A` |
| `onPrimaryContainer` | `#EFDBFF` | | `surfaceContainerHigh` | `#2A2531` |
| `secondaryContainer` | `#4C3D63` | | `surfaceContainerHighest` | `#342D40` |
| `onSecondaryContainer` | `#E9DDFB` | | `onSurface` | `#E8E0EE` |
| `tertiary` | `#F2B5D8` | | `onSurfaceVariant` | `#CDC4D6` |
| `tertiaryContainer` | `#5C3A4E` | | `outline` | `#4C4456` |

### 2.3 Ultraviolet — Light (fallback)

| Role | Hex | | Role | Hex |
|---|---|---|---|---|
| `primary` | `#6E2EC9` | | `surface` | `#FDF7FF` |
| `onPrimary` | `#FFFFFF` | | `surfaceContainerLow` | `#F6ECFC` |
| `primaryContainer` | `#EFDBFF` | | `surfaceContainer` | `#EFE3F8` |
| `onPrimaryContainer` | `#270056` | | `surfaceContainerHigh` | `#E9DDF4` |
| `secondaryContainer` | `#ECDDFB` | | `onSurface` | `#1D1B20` |
| `onSecondaryContainer` | `#211A2C` | | `onSurfaceVariant` | `#4A454E` |
| `tertiary` | `#7C5267` | | `outline` | `#7B757F` |

### 2.4 Semantic (non-M3-role) status colours

Connection/state colours, tuned per mode (do **not** rely on colour alone — pair with icon/label):

| Meaning | Dark | Light |
|---|---|---|
| Connected / success | `#7FD49B` | `#2E7D4F` |
| Resuming / pairing / warn | `#F2C14E` | `#8A6500` |
| Off-network | use `outline` | use `outline` |

### 2.5 Usage rules

- `primary` is the one accent: the OK ring, the active segment, the play button, selected nav. Not every
  control.
- Default controls are neutral `surfaceContainer*` fills with `onSurface` glyphs; they earn `primary`
  only when active/pressed.
- App brand colours appear **only as the app's own icon/tile**, never as a chip/surface fill.

---

## 3. Typography

M3 Expressive favours a tight, heavier scale. Keep the system font; formalise the emphasized scale
(unchanged from the prior `Type.kt`, still valid):

```kotlin
val AppTypography = Typography(
    displaySmall   = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.4.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 26.sp, lineHeight = 30.sp),
)
```

The now-playing/cover app name uses `titleLarge`; control labels `labelLarge`; the number pad
`headlineMedium`. Never below 12sp.

---

## 4. Shape & shape-morphing

### 4.1 Shape scale (`Shape.kt`, unchanged)

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(20.dp),   // control buttons, sheet rows
    large      = RoundedCornerShape(28.dp),   // cards, sheet
    extraLarge = RoundedCornerShape(36.dp),
)
```

### 4.2 Press morph (signature)

Controls morph rounder + scale down on press, settle on release; haptic on press-down; ripple off
(the morph is the feedback). Keep the existing `Modifier.pressMorph` primitive.

*Expressive upgrade:* use `MaterialShapes` + `Morph` for true shape-morphing (the OK ring morphs
circle → squircle on press; selected nav/segment fills via the Material Symbols **Fill** axis).

---

## 5. Elevation, recess & spacing

### 5.1 Tonal elevation, toned shadows

Separate layers with tonal `surfaceContainer*`, **not** heavy shadows. Drop shadows are deliberately
light app-wide (e.g. now-playing tile ≈ `0 8px 18px -14px` equivalent; phone-level cards minimal).

### 5.2 The gesture pad (flat tonal M3, not skeuomorphic)

The home gesture pad is a **flat tonal M3 surface** (`surfaceContainerHigh`, 28dp corners) — depth comes
from the tonal step against the `surface` background, not from inner shadows, mesh, or rubber textures
(a skeuomorphic recessed/rubber treatment was tried and dropped; snapshot at git tag
`experiment/skeuomorphic-clickpad`). The centre **OK** is an M3 Expressive filled-tonal key
(`primaryContainer`) that shape-morphs (circle → rounded-square) and scales on press. Directional
chevrons + corner actions are faint at rest and brighten on touch.

### 5.3 Spacing (4dp base) & touch targets

`Space { xs=4 s=8 m=12 l=16 xl=20 xxl=24 xxxl=32 }`. Screen padding 16–20dp; section gap 24dp. Minimum
tappable **48dp** (`Modifier.minimumInteractiveComponentSize()` on custom clickables); the OK ring and
transport play are larger.

---

## 6. Motion

Prefer the Expressive `MotionScheme` springs; keep `MotionSpecs` as the fallback vocabulary:

```kotlin
object MotionSpecs {
    val spatialFast    = spring<Float>(dampingRatio = 0.55f, stiffness = 1400f) // press
    val spatialDefault = spring<Float>(dampingRatio = 0.70f, stiffness = 700f)  // sheet / panel
    val spatialDp      = spring<Dp>(dampingRatio = 0.55f, stiffness = 1400f)    // corner morph
    val effects        = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
    val emphasized     = tween<Float>(durationMillis = 450, easing = EaseInOutCubic) // sheet/screen
}
```

- Command sheet rise/settle: `spatialDefault` spring.
- Press: `spatialFast` scale + corner morph; haptic on down.
- Connection chip "Resuming…" and the live dot: gentle pulse; **respect reduce-motion** (drop to static).

---

## 7. Iconography — Material Symbols Rounded

- Style **Rounded** (pairs with Expressive's rounded language). Use the **Fill** axis for
  selected/active (e.g. active sheet segment, current nav, playing state) — unfilled → filled
  transition on selection.
- Replace ad-hoc glyphs with the symbol set: `play / pause / stop / fast_forward / fast_rewind /
  volume_up / volume_down / volume_off / mic / close / speaker / tv / bluetooth / headphones / check /
  keypad / apps / input / keyboard`.
- Icon-only controls MUST carry a `contentDescription`.

---

## 8. Component specs

### 8.1 Gesture pad (home)
Flat tonal M3 surface (§5.2) filling the screen between the top bar and the grip. Glide = pointer (reuse the
motion-cursor engine); tap = click (centre OK affordance). Faint directional hints; edge zones drive
volume (right) / channel (left) with hold-to-repeat; edge swipe-in = Back. The OK affordance is the one
`primary` element and shape-morphs on press.

### 8.2 Command sheet
M3 bottom sheet (`large` top corners) with a grip handle and a segmented switch: **Keypad** (0–9 + the
four colour keys + Guide), **Apps** (launcher grid), **Inputs** (source list, active checked), **Sound**
(audio-output picker, §8.4), **Type** (on-screen keyboard / motion-pad assist). Opens to last-used
segment; selected segment uses `primary` fill + Material Symbols Fill.

### 8.3 Now-playing bar + cover view *(004)*
Bar docks above the pad when media plays: app icon + name + play-state + a play/pause mini button. Tap →
immersive **cover view**: large app tile (or real artwork, §8.5), app name (`titleLarge`), play-state,
transport row (rewind · play/pause · stop · fast-forward), and the sound-output chip. **No scrubber, no
next/prev** (SSAP gives no position). Dismiss with an **✕** (not a chevron).

### 8.4 Audio output picker *(006)*
Output chip (speaker/device icon) → picker listing TV speakers / Soundbar (ARC) / Bluetooth /
headphones / optical with Material Symbols device icons; active output checked; an output is dimmed
"unavailable" **only after** the TV rejects it.

### 8.5 Artwork view *(004)*
When the source provides art (Live TV channel logo, music, DLNA), the cover view shows a full-bleed art
backdrop with a bottom scrim and the controls overlaid. Otherwise the app tile. **Never** fabricate art.

### 8.6 Media notification *(009)*
MediaStyle notification, **V1 full player**: five buttons — mute · vol− · play/pause · vol+ · stop;
collapsed promotes vol− · play/pause · vol+. App icon + TV name + play-state; the system **output chip**
is the §8.4 picker. **No progress bar** (no duration set — renders like a live broadcast). Lock screen
and shade render the identical card.

### 8.7 Connection chip *(008)*
Top chip: **Connected** (success dot) / **Resuming…** (warn dot, during the grace window — no disconnect
screen) / **Off network** (outline dot + retry). The full reconnect screen appears only after a real,
sustained failure.

### 8.8 First-run teaching card
One-time, dismissible card over the pad teaching the two edge gestures (right = volume, left = channel).
Never shown again once dismissed.

---

## 9. Screen structure (Direction C, portrait)

```
┌─────────────────────────────┐
│ ● Living Room TV   🎤  ⏻     │  top bar: connection chip + voice + power   (§8.7)
├─────────────────────────────┤
│ [ now-playing bar ]          │  appears only when media is live           (§8.3)
│                              │
│        ╭───────────╮         │
│   ch ◀ │  ( OK )   │ ▶ vol   │  flat tonal gesture pad: glide/tap,         (§8.1)
│        ╰───────────╯         │  edges = channel/volume
│                              │
│        ▁▁▁ More controls     │  grip → command sheet                       (§8.2)
└─────────────────────────────┘
        ↑ pull ↑
┌─────────────────────────────┐
│ Keypad · Apps · Inputs · Sound · Type │  command sheet segments
└─────────────────────────────┘
```

---

## 10. Accessibility

- Light **and** dark must meet **WCAG AA**; if a dynamic palette would fall below AA, clamp roles rather
  than ship it.
- Custom `clickable` controls MUST set `Role.Button` and `minimumInteractiveComponentSize()` (≥48dp).
- Icon-only controls need `contentDescription`; never convey state by colour alone (pair icon+label).
- Respect **reduce motion**: press morph → simple state change, kill the live pulse.

---

## 11. Build order (suggested)

1. **Theme**: BOM bump, `UltravioletLight/Dark` schemes, dynamic-colour resolution, `MaterialExpressiveTheme`. (No behaviour change.)
2. **Icons**: Material Symbols Rounded swap across existing controls.
3. **Gesture pad** (flat tonal surface + edges) reusing the motion-cursor engine; retire the old touchpad.
4. **Command sheet** (segments) absorbing today's keypad/apps/inputs/bottom-bar; add **Sound** (006).
5. **Now-playing bar + cover/artwork** (004) restyled.
6. **Media notification V1** (009) + output picker; **connection chip** (008).
7. First-run card; reduce-motion + a11y pass; refresh Roborazzi goldens.

---

## Appendix — design reference artifacts

In [`docs/design/`](./design/) (open in a browser, toggle light/dark):

- `direction-c.html` — core system: home gesture pad, command sheet, first-run, connection states.
- `direction-c-features.html` — now-playing, cover & artwork views, audio output, notification, reconnect.
- `notification-controls.html` — lock-screen + shade + output picker (V1).
- `structure-rationale.html` — why Direction C over bottom-nav / scroll / gesture-only.
