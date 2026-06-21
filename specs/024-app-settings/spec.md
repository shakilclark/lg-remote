# Feature Specification: App Settings Interface

**Feature Branch**: `024-app-settings`

**Created**: 2026-06-21

**Status**: Draft

**Relates to / depended on by**: `021-theme-picker` (the Appearance picker lives as a sub-screen of
this Settings interface), `007-haptic-feedback`, `013-open-source-distribution` (licenses),
`010-expressive-redesign` (M3 Expressive theming). **Distinct from** the existing *TV* settings action
(`onOpenTvSettings`, which launches webOS settings on the TV) — this spec is the **app's own**
preferences.

**Input**: User description: "app settings interface, theme picker depends on this. research best
practise for modern material3 expressive settings."

## User Scenarios & Testing

### User Story 1 - Open Settings and navigate grouped sections (Priority: P1)

From the remote, the user opens an **App Settings** screen (its own navigation destination, not a
sheet) with a large collapsing top app bar titled "Settings" and a back arrow. Settings are organised
into a few **grouped sections** (M3 Expressive "segmented" tonal groups with a header per group,
spacing between groups rather than a divider between every row): TV / Connection · Appearance ·
Behaviour · About.

**Why this priority**: The screen and its IA are the container everything else hangs off; nothing else
in this spec exists without it.

**Independent Test**: Open Settings → see the grouped sections with a collapsing large app bar and a
working back navigation.

**Acceptance Scenarios**:
1. **Given** the remote, **When** the user opens App Settings, **Then** a dedicated screen appears with
   a large top app bar that collapses on scroll and a back affordance.
2. **Given** Settings is open, **When** the user scrolls, **Then** related rows read as grouped sections
   with headers, not an undifferentiated list.

### User Story 2 - Manage the TV connection (Priority: P1)

A **TV / Connection** group shows the connected TV (name · IP) and offers: re-scan for TVs, re-pair,
and **Forget this TV** (destructive — confirmed via dialog).

**Why this priority**: Connection management is the most-used real setting for a remote and currently
has no home.

**Acceptance Scenarios**:
1. **Given** a paired TV, **When** the user opens Settings, **Then** the connected TV (name + IP) is
   shown.
2. **Given** the TV group, **When** the user taps Forget this TV, **Then** a confirm dialog appears and,
   on confirm, the pairing is cleared.
3. **Given** the TV group, **When** the user taps Re-scan / Re-pair, **Then** discovery / pairing runs.

### User Story 3 - Reach the Appearance (theme) picker (Priority: P1)

An **Appearance** row (showing the current value, e.g. "System · Ultraviolet") navigates to an
**Appearance sub-screen** that hosts the theme picker (`021`): Light/Dark/System + the statement themes
+ Dynamic (wallpaper) on Android 12+.

**Why this priority**: `021` depends on this; the picker needs a home, and a sub-screen keeps the main
list short while giving the picker room to grow.

**Acceptance Scenarios**:
1. **Given** Settings, **When** the user taps Appearance, **Then** the theme/appearance sub-screen
   (`021`) opens and changes apply app-wide.
2. **Given** a theme is chosen, **When** the user returns to Settings, **Then** the Appearance row's
   supporting text reflects the current mode + theme.

### User Story 4 - Behaviour toggles (Priority: P2)

A **Behaviour** group with whole-row toggles/actions: Haptic feedback (on/off — `007`), Reset first-run
hints, and a placeholder for Wake-on-LAN (disabled "coming soon" until its spec lands).

**Acceptance Scenarios**:
1. **Given** the Behaviour group, **When** the user toggles Haptic feedback, **Then** the preference
   persists and app haptics respect it.
2. **Given** Reset first-run hints, **When** tapped, **Then** the gesture-hint/first-run flags reset.

### User Story 5 - About (Priority: P2)

An **About** group: app version (name + build), Open-source licenses (`013`, → a licenses sub-screen),
and source/links.

**Acceptance Scenarios**:
1. **Given** About, **When** the user views it, **Then** the version is shown and Licenses opens a
   readable list.

### User Story 6 - Accessible, persisted (Priority: P3)

Rows are whole-row interactive (toggle rows use `Role.Switch`, choice rows `Role.RadioButton`, nav rows
`Role.Button`), ≥48dp, with merged semantics so title+description read as one node. All preferences
persist via DataStore and survive relaunch.

**Acceptance Scenarios**:
1. **Given** TalkBack, **When** focus lands on a toggle row, **Then** it announces as one switch with
   its label and state.
2. **Given** any changed setting, **When** the app is relaunched, **Then** the value is retained.

### Edge Cases

- No TV paired yet → the TV/Connection group shows a "connect a TV" affordance instead of name/IP.
- Forget TV while connected → drop the connection and return to the connect flow.
- Disabled "coming soon" rows (WoL) must read as inactive, not broken (consistent with spec 020).

## Requirements

### Functional Requirements

- **FR-001**: App Settings MUST be its own navigation destination with a large collapsing top app bar
  and back navigation (not a bottom sheet or dialog).
- **FR-002**: Settings MUST present grouped sections (TV/Connection, Appearance, Behaviour, About) with
  per-group headers and group separation by spacing/segmented containers, not per-row dividers.
- **FR-003**: The TV/Connection group MUST show the connected TV (name + IP) and offer re-scan,
  re-pair, and Forget this TV (destructive, dialog-confirmed).
- **FR-004**: An Appearance row MUST navigate to the `021` theme/appearance picker (sub-screen) and
  reflect the current mode + theme as supporting text.
- **FR-005**: A Behaviour group MUST provide a Haptic-feedback toggle (`007`) and Reset-first-run-hints
  action; placeholders for unshipped features MUST render as clearly-inactive (per `020`).
- **FR-006**: An About group MUST show the app version and link to open-source licenses (`013`).
- **FR-007**: Setting rows MUST be whole-row interactive with correct roles (Switch/RadioButton/Button),
  ≥48dp targets, and merged semantics; the control's own handler is null when the row owns the action.
- **FR-008**: All preferences MUST persist via DataStore and apply on next launch.
- **FR-009**: This app-settings surface MUST be distinct from the existing *TV* settings launch
  (`onOpenTvSettings`); both MAY be reachable but MUST be clearly differentiated.
- **FR-010**: The interface MUST be built Compose-native (M3 `ListItem`-based rows) or an actively-
  maintained Compose settings library; it MUST NOT use the maintenance-mode AndroidX `Preference`
  (XML/Fragment) library.

### Key Entities

- **Settings group**: a titled cluster of rows (TV/Connection, Appearance, Behaviour, About).
- **Setting row types**: navigation (→ sub-screen / link, chevron), toggle (Switch), choice
  (radio/segmented), info (value only), destructive (→ confirm dialog).
- **Preferences** (DataStore): theme/mode (`021`), haptics on/off (`007`), first-run/hint flags, plus
  derived connection info (read from existing connection state).

## Success Criteria

- **SC-001**: A user can reach and change any app preference (theme, haptics, forget TV) from a single
  Settings entry point in ≤2 navigations.
- **SC-002**: Settings reads as grouped sections, not a flat list; the large app bar collapses on
  scroll.
- **SC-003**: All preferences survive relaunch.
- **SC-004**: Every row is operable by TalkBack as a single labelled control of the correct role, ≥48dp.

## Assumptions

- Recommended architecture (per research): one Settings destination + large/flexible collapsing top app
  bar; M3 Expressive segmented grouped sections; `ListItem` rows with `Switch`/chevron/segmented
  controls; **Appearance is a sub-screen** hosting `021`; **About→Licenses** a second sub-screen.
  Compose-native rows (a small `SettingsRow`/`SettingsGroup` set) are the default; the
  `alorma/compose-settings` `ui-tiles-expressive` module is an acceptable shortcut for the segmented
  look. AndroidX `Preference` is explicitly out.
- Depends on the Compose-BOM bump to stable M3 Expressive for the segmented `ListItem`/flexible app bar
  (audit P2); can ship on standard M3 components until then.
- Navigation: a single new destination (+ Appearance and Licenses sub-screens); the app currently has
  no nav graph, so a minimal one (or simple state-based screen switch) is introduced here.
- Connection actions (re-scan/re-pair/forget) reuse the existing discovery/pairing/store paths.
