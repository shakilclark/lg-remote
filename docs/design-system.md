# LG Remote — Design System (Material 3 Expressive)

Premium, native-Android design spec for the webOS remote UI. Built **on top of the existing
electric-red dark brand** (`ui/theme/Color.kt`) and expressed through **Material 3 Expressive**
— shape morphing, motion physics, emphasized typography, tonal elevation. The goal is
Samsung One-UI-grade polish that still feels like *this* app, not a generic Material template.

> Hand-off note: this is a spec for the Compose UI being built in `android/`. Token snippets are
> ready to paste into `com.shakilclark.lgremote.ui.theme`. Nothing here changes app logic.

---

## 0. Enabling M3 Expressive (dependency bump)

The project is currently on **Compose BOM `2024.12.01`** (Material3 `1.3.x`) — that is *stable M3*,
but the new Expressive components (`ButtonGroup`, expressive `LoadingIndicator`, shape-morph
`IconButton`s, the spring `MaterialMotion` specs, etc.) and `MaterialShapes` land in
**Material3 `1.4.0-alpha+` / `1.5.0`**.

Two ways to proceed:

- **Ship now on 1.3.x** — every token below (color, type, shape, spacing, elevation) and the custom
  components work on the current BOM. Motion uses `androidx.compose.animation` springs directly.
  This is the recommended default; nothing is blocked.
- **Opt into Expressive components** — bump the catalog:

  ```toml
  # gradle/libs.versions.toml
  composeBom = "2025.06.01"   # or newer; pulls Material3 1.4.x
  # if Expressive APIs are still flagged in your BOM, pin material3 directly:
  # androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3", version = "1.4.0-alpha18" }
  ```

  Expressive APIs are `@ExperimentalMaterial3ExpressiveApi` until 1.5.0 — opt in per-file.

Everything below is written so the **look** is achievable on 1.3.x; Expressive APIs are called out as
*upgrade* where they make a component nicer.

---

## 1. Design principles

1. **One-handed, thumb-first.** A remote is held and operated by thumb. Primary controls live in the
   lower two-thirds; reach-critical targets (volume, D-pad, OK) are large and bottom-weighted.
2. **Tactile feedback over chrome.** Every press answers instantly — shape-morph + spring scale +
   haptic. The TV is across the room, so the *phone* must confirm the action.
3. **Dark-first, single accent.** Near-black canvas, one electric-red accent used sparingly for the
   live/active state and the OK center. Restraint is what reads as premium.
4. **Big, calm type.** Few words, high weight, generous spacing. No dense text on a remote.
5. **Motion is physical, not decorative.** Springs, not linear tweens. Things settle like objects.

---

## 2. Color

The existing palette is kept verbatim and mapped to the **full** M3 color-role set so every component
themes correctly. Dark-first; the app forces dark regardless of system setting.

### 2.1 Brand source (unchanged — `Color.kt`)

| Token | Hex | Role |
|---|---|---|
| `Bg` | `#0E0F13` | app canvas |
| `Panel` | `#181A21` | raised surface |
| `Panel2` | `#1F222B` | higher surface / pressed track |
| `Edge` | `#2B2F3A` | hairline outline |
| `Press` | `#2A2E39` | pressed fill |
| `TextPrimary` | `#F2F4F8` | primary text/icon |
| `Muted` | `#8B91A1` | secondary text |
| `Accent` | `#C0123A` | deep electric red |
| `AccentSoft` | `#E23A5E` | bright red (primary) |
| `Ok` | `#34D27B` | connected/success |
| `Warn` | `#F0B429` | pairing/warning |

### 2.2 Full M3 role mapping (replace `DarkColors` in `Theme.kt`)

A complete `darkColorScheme` so M3 components (snackbars, dialogs, chips, the upgrade-path Expressive
widgets) inherit the brand instead of falling back to purple defaults.

```kotlin
private val DarkColors = darkColorScheme(
    primary            = AccentSoft,          // #E23A5E — main interactive accent
    onPrimary          = Color(0xFF1A0309),
    primaryContainer   = Color(0xFF7A0C25),   // deep red container (OK btn, live chip)
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary          = Color(0xFFB9C0CF),   // neutral controls
    onSecondary        = Color(0xFF1A1D24),
    secondaryContainer = Panel2,
    onSecondaryContainer = TextPrimary,
    tertiary           = Ok,                   // connected accent
    onTertiary         = Color(0xFF06170E),
    background         = Bg,
    onBackground       = TextPrimary,
    surface            = Bg,
    onSurface          = TextPrimary,
    surfaceContainerLowest = Color(0xFF0A0B0E),
    surfaceContainerLow    = Panel,           // #181A21
    surfaceContainer       = Color(0xFF1C1F27),
    surfaceContainerHigh   = Panel2,          // #1F222B
    surfaceContainerHighest= Color(0xFF262A34),
    surfaceVariant     = Panel2,
    onSurfaceVariant   = Muted,
    outline            = Edge,                 // #2B2F3A
    outlineVariant     = Color(0xFF22252E),
    error              = AccentSoft,
    onError            = Color(0xFF1A0309),
    scrim              = Color(0xCC000000),
)
```

> **Dynamic color note:** Material You wallpaper theming is *off* by design — the electric-red brand
> is the identity. Don't wire `dynamicDarkColorScheme()`. (If a future "match my wallpaper" toggle is
> wanted, gate it behind a setting and keep red as default.)

### 2.3 Usage rules

- **Accent (`primary`/`AccentSoft`) is rare.** Reserved for: OK center, the live "connected" pulse,
  and the active/pressed transport state. Don't tint every button red.
- **Buttons are neutral by default** — `surfaceContainerHigh` fill, `onSurface` glyph. They earn
  accent only while pressed.
- **Status colors:** `tertiary/Ok` = connected, `error/AccentSoft` = disconnected, `Warn` = pairing.

---

## 3. Typography

Expressive favors a tighter, heavier scale for controls. Keep the system font (clean, native) and
formalize an emphasized scale. Replace `AppTypography` in `Type.kt`:

```kotlin
val AppTypography = Typography(
    // Display — the connected TV name / big idle clock if used
    displaySmall = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    // Section headers ("Playback", "Apps")
    titleLarge   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleMedium  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    // Body / connection details
    bodyLarge    = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium   = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    // Button & key labels (emphasized)
    labelLarge   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium  = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.4.sp),
    // The big number/channel cluster — heavy, tabular feel
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 26.sp, lineHeight = 30.sp),
)
```

Rules: control labels use `labelLarge`; the OK glyph and channel numbers use `headlineMedium`;
the TV name in the top bar uses `titleMedium`. Never go below 12sp.

---

## 4. Shape & shape-morphing

Shape is the headline Expressive feature. Two layers:

### 4.1 Static shape scale (works on 1.3.x — add `Shape.kt`)

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),    // chips, status pills
    small      = RoundedCornerShape(14.dp),   // small keys
    medium     = RoundedCornerShape(20.dp),   // standard control buttons
    large      = RoundedCornerShape(28.dp),   // panels / cards
    extraLarge = RoundedCornerShape(36.dp),   // the D-pad cluster container
)
```

Add `shapes = AppShapes` to the `MaterialTheme(...)` call in `Theme.kt`.

- **Buttons:** `medium` (20dp) resting.
- **OK center & volume rocker:** fully rounded (`CircleShape` / `RoundedCornerShape(50)`).
- **Cluster containers:** `extraLarge` (36dp) for a soft, modern superellipse feel.

### 4.2 Press morph (the tactile signature)

On press, controls **morph rounder + scale down** with a spring, then settle back. This is the
single most important "premium" detail on a remote.

- Resting → pressed: corner radius `20dp → 28dp`, scale `1.0 → 0.94`.
- Driven by a `spring(dampingRatio = 0.55f, stiffness = 700f)` (see §6).
- Pair with a haptic `HapticFeedbackType.LongPress` on press-down.

*Upgrade (1.4+):* use `MaterialShapes` + `RoundedPolygon`/`Morph` for true shape-morphing buttons
(e.g. circle → squircle → cookie on press). Until then the corner+scale spring reads almost as good.

A reusable press-morph modifier (works on 1.3.x):

```kotlin
@Composable
fun Modifier.pressMorph(
    interaction: InteractionSource,
    pressedCorner: Dp = 28.dp,
    restCorner: Dp = 20.dp,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) pressedCorner else restCorner, MotionSpecs.spatialFast, label = "corner")
    val scale  by animateFloatAsState(if (pressed) 0.94f else 1f, MotionSpecs.spatialFast, label = "scale")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(RoundedCornerShape(corner))
}
```

---

## 5. Elevation, spacing & layout grid

### 5.1 Elevation — tonal, not shadow

Dark Expressive UIs separate layers with **tonal surface containers**, not heavy drop shadows.

| Layer | Surface role |
|---|---|
| Canvas | `background` (`#0E0F13`) |
| Resting button | `surfaceContainerHigh` (`#1F222B`) |
| Cluster panel | `surfaceContainerLow` (`#181A21`) + 1dp `outlineVariant` hairline |
| Pressed | `surfaceContainerHighest` + accent tint |

Use at most a `2dp` shadow on the floating bottom action bar; everything else is tonal.

### 5.2 Spacing scale (4dp base)

`4 · 8 · 12 · 16 · 20 · 24 · 32`. Define as an object:

```kotlin
object Space { val xs=4.dp; val s=8.dp; val m=12.dp; val l=16.dp; val xl=20.dp; val xxl=24.dp; val xxxl=32.dp }
```

- Screen padding: `20dp` horizontal.
- Inter-control gap in the D-pad/keys: `12dp`.
- Section gap: `24dp`.

### 5.3 Touch targets

- **Minimum** any tappable: `48dp`.
- **Primary keys** (volume, channel, D-pad arrows): `64dp`.
- **OK center:** `80dp`.
- The motion-cursor toggle and transport row: `56dp`.

---

## 6. Motion (physics, not tweens)

Define a `MotionSpecs` object and use it everywhere — consistency is what sells "designed".

```kotlin
object MotionSpecs {
    // Spatial = anything that moves/resizes/scales (springs)
    val spatialFast    = spring<Float>(dampingRatio = 0.55f, stiffness = 1400f)  // button press
    val spatialDefault = spring<Float>(dampingRatio = 0.70f, stiffness = 700f)   // panel/cluster
    val spatialDp      = spring<Dp>(dampingRatio = 0.55f, stiffness = 1400f)     // corner morph
    // Effects = color/alpha (no overshoot)
    val effects        = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
    val emphasized     = tween<Float>(durationMillis = 450, easing = EaseInOutCubic) // screen transitions
}
```

- **Press:** scale + corner via `spatialFast` (snappy, slight overshoot). Haptic on down.
- **Connection pulse:** the live dot breathes with an `infiniteRepeatable` alpha `0.4↔1.0`, 1200ms.
- **Screen/sheet transitions:** `emphasized` for enter/exit; slide + fade, never a hard cut.
- **Volume/channel repeat:** holding a key repeats the SSAP command every ~120ms with a subtle
  pulsing scale so the user sees it's firing.

*Upgrade (1.4+):* swap the tween transitions for `MaterialMotion`/`MotionScheme.expressive()` springs.

---

## 7. Component specs

Concrete recipes for the remote's actual controls. All reference the tokens above.

### 7.1 Control key (the workhorse button)

- Shape `medium` (20dp), fill `surfaceContainerHigh`, glyph `onSurface`, size `64dp`.
- Pressed: `pressMorph` (28dp corner, 0.94 scale), fill shifts to `surfaceContainerHighest` with a
  6% `primary` overlay, glyph → `primary`.
- Haptic `LongPress` on press-down. Ripple **off** (the morph is the feedback) — use
  `indication = null` with an `InteractionSource`.

### 7.2 D-pad cluster

- A single `extraLarge` (36dp) `surfaceContainerLow` panel, ~`260dp` square, with a hairline
  `outlineVariant` border.
- Four arrow keys (Up/Down/Left/Right) as `64dp` targets at the edges; **OK** is an `80dp` circle in
  the center filled `primaryContainer` with `onPrimaryContainer` glyph — the only persistently
  accented control.
- Arrows use the standard control-key morph; OK gets a slightly stronger press (scale `0.92`,
  brief `primary` ring that springs out on release).
- Optional: long-press OK morphs the cluster into the **motion-cursor** affordance (a subtle scale of
  the whole panel + label swap) to tie US5 into the same surface.

### 7.3 Volume / channel rockers

- Two vertical pill rockers (`RoundedCornerShape(50)`), `surfaceContainerHigh`, `64dp` wide.
- `+`/`−` (vol) and `▲`/`▼` (ch) stacked; press lights the half being pressed with `primary` tint.
- Mute is a separate round toggle: when muted, fills `error`/`AccentSoft` with a slash icon and the
  whole pill desaturates.
- Hold-to-repeat with the §6 repeat pulse.

### 7.4 Transport row (play/pause/seek)

- Horizontal row of `56dp` round controls on a `large` panel.
- Play/Pause is a single morphing button: the **icon** cross-fades play↔pause (`effects` tween) while
  the button does the press morph. *Upgrade:* shape-morph the container too.

### 7.5 App shortcut chips (YouTube / Netflix — US6)

- Wide `extraSmall`→`small` chips, `surfaceContainerHigh`, brand glyph + `labelLarge` text.
- These may carry their brand color *as the icon only*; chip fill stays neutral so the screen doesn't
  turn into a logo soup. Pressed: standard morph + subtle scale.

### 7.6 Top bar — connection status (US1)

- `titleMedium` TV name + a status cluster on the right:
  - **Connected:** `tertiary/Ok` dot with the breathing pulse + "Connected".
  - **Pairing:** `Warn` dot, "Pairing…" with an Expressive `LoadingIndicator` (1.4+) or a
    `CircularProgressIndicator` tinted `Warn` (1.3.x).
  - **Disconnected:** `error` dot + "Reconnect" text button.
- Tapping the cluster opens the connection sheet (IP entry / re-pair).

### 7.7 Input switcher (US7)

- A bottom sheet (`large` top corners) listing HDMI/inputs as full-width rows with a leading icon and
  a trailing `primary` check on the active input. Rows use the control-key press tint.

---

## 8. Screen layout (portrait, one-handed)

```
┌─────────────────────────────┐
│  ◐ LG C3  •Connected   ⚙    │  top bar (status + settings)   ~64dp
├─────────────────────────────┤
│                             │
│     [▲]                     │
│  [◀] (OK) [▶]   D-pad       │  cluster (extraLarge panel)    ~260dp
│     [▼]                     │
│                             │
├──────────────┬──────────────┤
│  VOL  ┃ CH    │  ▶ ⏸ ⏮ ⏭   │  rockers + transport
│  [+]  ┃ [▲]   │              │
│  [−]  ┃ [▼]   │  [mute]      │
├─────────────────────────────┤
│  ▶ YouTube   ◼ Netflix      │  shortcut chips
├─────────────────────────────┤
│  ◰ Inputs    ✛ Motion       │  floating action bar (2dp)
└─────────────────────────────┘
```

Reach budget: D-pad + OK and the volume rocker sit in the natural thumb arc (lower-center to
lower-left). Less-used actions (inputs, settings) sit at the extremes.

---

## 9. Accessibility

- All icon-only controls need `contentDescription` (e.g. "Volume up").
- Min target 48dp (we exceed it on primary keys).
- Don't rely on the red accent alone for state — pair with icon/label (mute icon, "Connected" text).
- Respect `Reduce motion`: when set, drop the press morph to a simple alpha/`indication` and disable
  the infinite pulse.
- Contrast: `onSurface (#F2F4F8)` on `surfaceContainerHigh (#1F222B)` passes AA; keep `Muted` for
  secondary text only, never for the primary label on a button.

---

## 10. Build order (suggested)

1. Land tokens: `Color.kt` role expansion, `Type.kt`, new `Shape.kt`, `MotionSpecs`, `Space`,
   wire `shapes` into `Theme.kt`. (No behavior change — safe first commit.)
2. Build `ControlKey` + `pressMorph` as the shared primitive; preview it.
3. Compose the D-pad cluster + OK.
4. Volume/channel rockers + mute.
5. Transport row + app chips.
6. Top-bar status + connection sheet, input sheet.
7. (Optional) bump BOM and swap in Expressive components/motion where §4.2/§6 note an upgrade.

---

### Tooling to generate against this spec

If you want to spin screens visually before hand-coding: **Google Stitch** or **Google AI Studio
(Build Android apps)** to draft the layout, then **Android Studio's Gemini agent** (screenshot of the
chosen mock → Compose). Feed any of them §2–§6 as the constraint set so output stays on-brand.
Builder.io **Visual Copilot** is the path if a Figma file ever becomes the source of truth.
