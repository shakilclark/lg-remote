# Feature Specification: Great User-Facing README

**Feature Branch**: `018-readme`

**Created**: 2026-06-20

**Status**: Draft

**Relates to**: `013-open-source-distribution` (the README is the public repo's storefront),
`015-app-name` (the README headlines the chosen brand, not "LG"), and `010-expressive-redesign`
(screenshots come from the redesigned app, light + dark).

**Input**: "Research and spec a great user-facing GitHub README, with great visuals, not too long."

## Why this feature (context)

When the repo goes public (`013`), the README is the first — often only — thing a visitor reads. The
current README is developer/PWA-era notes, not a storefront. Research is consistent: repos with
screenshots get markedly more engagement, the README should be **read in seconds** (hero + visual demo
up top), and it should be **short** (~500–1500 words) with depth linked out rather than dumped in. This
feature specifies a concise, visual, user-facing README that sells the app and gets people installing.

### Research summary (what a great app README does)

- **Hero first**: centered wordmark/logo + project name + a one-line value proposition + a small row of
  badges + a **demo GIF or screenshot** — so the app is understood at a glance.
- **Visuals win**: screenshots (light **and** dark) near the top; a short looping demo for the
  gesture/interaction; host images in `docs/` (or GitHub CDN).
- **3-step quick start**: copy-paste install, nothing more, up high.
- **Scannable**: compact feature bullets/table, a short "why", then links out (Contributing, docs,
  design system) — **don't** dump the dev journal.
- **Trust**: 4–7 meaningful badges (build, license, latest release, store), not decoration.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Understand it in seconds (Priority: P1)

A visitor lands on the repo and within seconds knows what the app is and what it looks like — from a
hero (brand name + one-line pitch) and screenshots (light + dark) plus a short demo of the gesture
remote, without reading paragraphs.

**Why this priority**: First impression decides whether they keep reading or bounce.

**Acceptance Scenarios**:

1. **Given** the repo front page, **When** it loads, **Then** the top shows the brand name, a one-line
   pitch, badges, and a visual (screenshot/GIF) above the fold.
2. **Given** the visuals, **When** viewed, **Then** both light and dark appearances are shown.

### User Story 2 - Install in three steps (Priority: P1)

A visitor can install the app from a Quick Start of at most three copy-paste steps (GitHub release /
Obtainium / store link) without hunting.

**Acceptance Scenarios**:

1. **Given** the README, **When** I reach Quick Start, **Then** there are ≤3 steps with copy-paste
   commands/links that actually install the app.

### User Story 3 - Scannable, short, with depth linked out (Priority: P2)

The README is scannable (compact features, a short "why", a compatibility note) and **short**; details
(build internals, architecture, contributing, design system) are linked, not inlined.

**Acceptance Scenarios**:

1. **Given** the README, **When** measured, **Then** it is ~500–1500 words and reads top-to-bottom in a
   minute; long-form content is behind links (CONTRIBUTING, `docs/`).

### Edge Cases

- Brand name not yet chosen (`015`) → the README MUST use the final brand, so it ships after/with the
  naming decision; "LG" appears only as a **compatibility** mention (nominative), never as the title.
- Light/dark images on GitHub → use the `<picture>`/`prefers-color-scheme` technique or show both, so
  the visuals look right in both GitHub themes.
- Screenshots must reflect the **shipped** redesign (`010`), not mockups, before the repo goes public.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The README MUST open with a hero: centered brand name/wordmark, a **one-line** value
  proposition, a small row of badges, and a visual (screenshot or demo GIF) above the fold.
- **FR-002**: It MUST show the app in **both light and dark**, with at least one short **demo GIF** of
  the gesture remote in action; images hosted in `docs/` (or GitHub CDN).
- **FR-003**: It MUST include a **Quick Start of ≤3 copy-paste steps** to install (GitHub release /
  Obtainium / store links per `013`).
- **FR-004**: Features MUST be presented as a **compact, scannable** list/table — not prose paragraphs.
- **FR-005**: It MUST stay **short** (~500–1500 words); build internals, architecture, contributing,
  and the design system MUST be **linked**, not inlined.
- **FR-006**: It MUST carry **4–7 meaningful badges** (e.g. build status, license, latest release, FOSS
  store), no badge clutter.
- **FR-007**: The title/brand MUST be the chosen name (`015`), never "LG"; LG/webOS appear only as a
  **compatibility** note (nominative fair use).
- **FR-008**: It MUST link to `CONTRIBUTING`, the `docs/` (incl. `design-system.md`), and the
  license.

### Key Entities

- **README hero**: brand + pitch + badges + lead visual.
- **Asset set**: light/dark screenshots + demo GIF in `docs/` referenced by the README.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can state what the app is and has seen it (screenshot/GIF) within
  ~10 seconds, above the fold.
- **SC-002**: Installation is achievable from the README's Quick Start in ≤3 steps.
- **SC-003**: README length is ~500–1500 words; no section is a dev-journal dump.
- **SC-004**: Visuals render correctly in both GitHub light and dark themes and show both app modes.
- **SC-005**: No "LG" in the title/brand; only a compatibility mention.

## Assumptions

- The brand name (`015`) and the shipped redesign (`010`) exist before the public README is finalised;
  a draft can be staged earlier with placeholders.
- Screenshots/GIF are captured from the real app (light + dark) and stored in `docs/`.
- GitHub Markdown + `<picture>` for theme-aware images is acceptable (no external rendering service).
