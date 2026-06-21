# Feature Specification: Statement Theme Picker

**Feature Branch**: `021-theme-picker`

**Created**: 2026-06-21

**Status**: Draft

**Relates to**: `010-expressive-redesign` (Material You theming, light/dark, Ultraviolet fallback),
`docs/design-system.md` §2, and `tools/theme/` (the seed→scheme generator — each statement theme is a
Material Theme Builder seed run through it).

**Depends on**: `024-app-settings` — the picker is now hosted as the **Appearance sub-screen** of the
app Settings interface (reached from a Settings row), per the settings research. The standalone
top-bar bottom-sheet described below is demoted to an OPTIONAL quick-switch shortcut; the canonical
home is Settings → Appearance. Where this spec says "bottom sheet / top-bar icon", read "Appearance
sub-screen (with an optional quick-switch sheet)".

**Input**: User description: "theme picker, with statement Material 3 Expressive designs: Obsidian
Glass, Willy Wonka, Vaporwave, Punk, Simply Red. Researched for UX patterns for the switcher, colour
themes."

## User Scenarios & Testing

### User Story 1 - Pick a statement theme with live preview (Priority: P1)

From the remote, the user opens an **Appearance** panel (a modal bottom sheet launched from a single
top-bar icon) and taps a **theme swatch card**. The whole app re-themes **instantly and live** — the
remote visible behind/below the translucent sheet recolours immediately, so the choice is WYSIWYG with
no separate mock and no "Apply" step.

**Why this priority**: This is the feature. A one-screen remote shouldn't bury theming in a settings
tree; a swatch gallery with instant live re-theme is the whole value.

**Independent Test**: Open the sheet, tap each of the five themes, confirm the app recolours instantly
and the active card is marked.

**Acceptance Scenarios**:
1. **Given** the remote is on screen, **When** the user taps the Appearance icon, **Then** a bottom
   sheet opens showing theme swatch cards (each = name + primary/secondary/tertiary chips).
2. **Given** the sheet is open, **When** the user taps a theme card, **Then** the app recolours
   immediately and a checkmark + outline marks the selected card.

### User Story 2 - Light / Dark / System mode, orthogonal to theme (Priority: P1)

A 3-option segmented control (Light / Dark / System) sits above the gallery. Mode and theme are
**independent axes**: the theme picks the seed/colorScheme; the mode picks which of that theme's
light/dark schemes is shown.

**Why this priority**: Users expect the light/dark axis first; it must compose cleanly with the
statement-theme choice rather than multiplying options.

**Independent Test**: Pick a theme, toggle Light/Dark/System, confirm the same theme renders in each
mode and "System" follows the OS setting.

**Acceptance Scenarios**:
1. **Given** a theme is selected, **When** the user picks Dark, **Then** the app shows that theme's
   dark scheme regardless of OS setting.
2. **Given** mode = System, **When** the OS switches light/dark, **Then** the app follows.

### User Story 3 - Dynamic (wallpaper) as a peer option (Priority: P2)

On Android 12+, a **Dynamic** card (multi-colour gradient chip) appears as the **first** item — the
Material You "follow my wallpaper" choice, layered as a peer of the statement themes. Hidden below
Android 12.

**Why this priority**: Preserves the existing Material You behaviour as an explicit, user-chosen option
rather than the only path; the statement themes are the new value but dynamic shouldn't be lost.

**Acceptance Scenarios**:
1. **Given** Android 12+, **When** the sheet opens, **Then** a Dynamic card is shown first and, when
   selected, the app uses `dynamic{Light,Dark}ColorScheme`.
2. **Given** Android < 12, **When** the sheet opens, **Then** no Dynamic card is shown.

### User Story 4 - Choice persists across launches (Priority: P2)

The (theme, mode) selection is persisted and re-applied instantly on next launch, surviving process
death.

**Independent Test**: Pick Vaporwave/Dark, kill the app, relaunch → it opens in Vaporwave/Dark with no
flash of the default.

**Acceptance Scenarios**:
1. **Given** a selection, **When** the app is killed and relaunched, **Then** the same theme + mode
   apply from first frame.

### User Story 5 - Accessible, never colour-alone (Priority: P3)

Selection is shown by **checkmark + outline + screen-reader `selected` state**, not colour alone. The
re-theme transition is animated (colour cross-fade + expressive spring) but **snaps instantly when
reduce-motion is on**. Every theme's `on*` roles meet contrast against their containers.

**Acceptance Scenarios**:
1. **Given** TalkBack is on, **When** focus lands on a card, **Then** it announces the theme name and
   selected state.
2. **Given** reduce-motion is enabled, **When** a theme is picked, **Then** the recolour is instant (no
   animation).

### Edge Cases

- High-chroma themes (Punk, Vaporwave) must still pass body-text contrast — verify each generated
  scheme and bump tones where needed; honour the OS contrast setting.
- Switching from Dynamic to a fixed theme (and back) must not require a relaunch.
- The sheet itself must remain legible mid-transition in every theme.

## Requirements

### Functional Requirements

- **FR-001**: The app MUST provide an Appearance entry point from the remote's top bar that opens a
  modal bottom-sheet theme picker (no full settings tree).
- **FR-002**: The picker MUST offer the five statement themes (Obsidian Glass, Willy Wonka, Vaporwave,
  Punk, Simply Red) as swatch cards showing each theme's key colours.
- **FR-003**: Selecting a theme MUST re-theme the entire app **instantly** (no Apply button), driven by
  a single theme state above `MaterialExpressiveTheme`.
- **FR-004**: The picker MUST expose a Light / Dark / System mode control that composes orthogonally
  with the theme choice.
- **FR-005**: On Android 12+, the picker MUST offer Dynamic (wallpaper) colour as a peer option, shown
  first; it MUST be hidden below Android 12.
- **FR-006**: The selected (theme, mode) MUST persist (DataStore) and apply on launch before first
  paint (no default-theme flash).
- **FR-007**: Active selection MUST be indicated by icon + outline + accessible `selected` semantics,
  never colour alone; cards MUST meet the 48dp touch target.
- **FR-008**: The re-theme transition MUST respect reduce-motion (snap, no animation, when enabled).
- **FR-009**: Each theme's colour scheme MUST be generated from a documented seed via `tools/theme/`
  (Material Theme Builder algorithm), with `on*` roles meeting WCAG contrast against their containers.
- **FR-010**: Themes MUST NOT rely on colour alone for any state meaning elsewhere in the app
  (consistent with design-system §2.4).

### Key Entities

- **Theme**: a named identity = a seed colour → full light + dark M3 tonal scheme. Candidate seeds
  (Material Theme Builder source colour; refine in-tool for contrast):

  | Theme | Seed | Accents (secondary / tertiary / signature) | Mood / personality |
  |---|---|---|---|
  | **Obsidian Glass** | `#5B6B8C` | `#8A93A6` · `#C9D2E3` · `#9FB4D9` | Dark, smoky, frosted-glass; cool/premium; low-chroma surfaces w/ blue tint, soft large corners, calm springs |
  | **Willy Wonka** | `#6B3A1F` | `#E0457B` · `#F2B705` · `#5FB36A` | Whimsical chocolate+candy; high-chroma accents; bouncy springs, exaggerated rounded shapes |
  | **Vaporwave** | `#FF4FD8` | `#00E5FF` · `#7A4DFF` · `#1B0B3B` | 80s/90s neon; magenta+cyan tension; dark-mode glow; snappy retro motion |
  | **Punk** | `#FF1F6B` | `#000000` · `#E6FF00` · `#FFFFFF` | High-contrast DIY zine; hot-pink on black/white, safety-yellow alert; sharp corners, hard motion |
  | **Simply Red** | `#D11A1A` | `#7A0F12` · `#FF6B5E` · `#2A0808` | Bold monochromatic red; single-hue family; warm red surface tint; decisive springs |

- **Appearance preference**: `(themeId, mode)` where `themeId ∈ {dynamic, obsidian, wonka, vaporwave,
  punk, red}` and `mode ∈ {light, dark, system}`. Persisted.

## Success Criteria

- **SC-001**: A user can change theme in ≤2 taps from the remote and see the result instantly.
- **SC-002**: 100% of themes pass WCAG AA for body text against their surfaces in both light and dark.
- **SC-003**: The chosen theme survives app kill/relaunch with no visible default-theme flash.
- **SC-004**: Selection state is perceivable without colour (icon + outline + TalkBack), and the
  transition honours reduce-motion.

## Assumptions

- Builds on the `010` theming layer; depends on the Compose-BOM bump to stable M3 Expressive
  (`MaterialExpressiveTheme`) — see audit P2 / `docs/audit-2026-06.md`. Until then it can ship on the
  current `MaterialTheme` with seeded schemes.
- Each theme ships as a pre-generated light+dark `ColorScheme` (via `tools/theme/`); themes are not
  user-editable in v1.
- The picker is a single bottom sheet, not a settings tree (the app is a focused one-screen remote).
- Switcher UX (bottom-sheet swatch gallery + orthogonal segmented mode + Dynamic-first card, instant
  apply, DataStore persistence) is per the researched recommendation; a dedicated Appearance screen is
  a deferred alternative if the theme list grows.
