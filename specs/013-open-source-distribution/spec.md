# Feature Specification: Open Source & Distribution

**Feature Branch**: `013-open-source-distribution`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the signed release workflow + assembled APK already exist).
Pairs with `014-remove-pwa-traces` (a clean, native-only repo is a prerequisite for a credible public
release).

**Input**: "Open-source it, add a GitHub Pages site, submit to open-source app stores, later the Play
Store." Research-first (per request).

## Why this feature (context)

The remote is a finished, *useful* native app with **no proprietary or Google-Play-Services
dependencies** (Compose, OkHttp, Coil, kotlinx — all FOSS) and **local-only** operation (no backend,
no tracking) — which makes it an ideal fit for the open-source Android ecosystem (F-Droid &
friends). This feature opens the source, gives people honest ways to install it, and lists it where
FOSS users actually look, deferring the Play Store (with its account/listing/testing overhead) to
later.

Research summary (sources): **IzzyOnDroid** is the lowest-friction store — it ingests the
developer-signed APK from tagged GitHub releases and verifies a **reproducible build**; **F-Droid**
needs a metadata merge-request into `fdroiddata` and prefers FOSS licenses + reproducible builds;
**Accrescent** takes a developer-submitted signed app via its console. All three benefit from
`fastlane/metadata/android` living in-repo. **Risk to flag**: Google announced developer-verification
for *sideloading* on certified devices by ~Sept 2026 — it may affect direct-APK installs and is worth
tracking. Apache-2.0 is the Android default; **GPLv3-or-later** is the F-Droid-native copyleft choice;
the licence is the user's call (recommended below).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The project is open source (Priority: P1)

The repository is public with a clear licence, a README that explains what the app is and shows
screenshots, and contribution/build instructions — so anyone can read, trust, build, and contribute.

**Why this priority**: Everything else (stores, landing page) depends on the repo being open and
legible first.

**Acceptance Scenarios**:

1. **Given** the repo, **When** someone visits it, **Then** it is public with a `LICENSE`, a README
   (what/why, screenshots, build steps), and `CONTRIBUTING`.
2. **Given** the dependency set, **When** audited, **Then** it contains no proprietary/Play-Services
   dependency that would block FOSS distribution.

### User Story 2 - Anyone can install it directly (Priority: P1)

Each release publishes a developer-signed APK on GitHub Releases, installable directly or via Obtainium
(which tracks GitHub releases) — the baseline path that needs no store.

**Acceptance Scenarios**:

1. **Given** a tagged release, **When** it is published, **Then** a signed APK + release notes are
   attached and install on a real device.
2. **Given** Obtainium, **When** pointed at the repo, **Then** it finds and installs the release and
   sees future updates.

### User Story 3 - A landing page (Priority: P2)

A GitHub Pages site presents the app — what it does, screenshots, install links (GH release / stores) —
at a shareable URL.

**Acceptance Scenarios**:

1. **Given** GitHub Pages is enabled, **When** I visit the project URL, **Then** a landing page shows
   the app with current install links.

### User Story 4 - Listed on IzzyOnDroid (Priority: P2)

The app is listed on IzzyOnDroid, built reproducibly from the tagged GitHub release, with store
metadata (description, screenshots) sourced from `fastlane/metadata/android` in the repo.

**Acceptance Scenarios**:

1. **Given** a reproducible release + `fastlane/metadata/android`, **When** submitted to IzzyOnDroid,
   **Then** it is accepted and marked reproducible, and updates flow from new releases.

### User Story 5 - F-Droid (and optionally Accrescent) (Priority: P3)

The app is submitted to F-Droid (metadata MR into `fdroiddata`) and optionally Accrescent, reaching the
widest FOSS audience.

**Acceptance Scenarios**:

1. **Given** a FOSS licence + reproducible build + metadata, **When** the `fdroiddata` request is
   filed, **Then** it meets the inclusion criteria (FOSS deps, builds from source).

### User Story 6 - Google Play, later (Priority: P3, deferred)

After the FOSS rollout, publish to the Play Store: developer account, AAB, store listing, and the
required pre-launch testing — explicitly a later phase.

**Acceptance Scenarios**:

1. **Given** a Play developer account, **When** the listing + AAB + testing requirements are met,
   **Then** the app passes review and is published.

### Edge Cases

- A release that isn't bit-for-bit reproducible → IzzyOnDroid still lists the signed APK but won't mark
  it reproducible; document what breaks determinism (timestamps, build env) and fix iteratively.
- Sideload developer-verification (Google, ~Sept 2026) → may require registering the signing identity;
  track and document the install path if it changes.
- Licence vs dependencies → the chosen licence MUST be compatible with all bundled FOSS deps.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The repository MUST be made public with a `LICENSE` file. *Recommended:* **GPL-3.0-or-later**
  (F-Droid-native copyleft); Apache-2.0/MIT acceptable if the user prefers permissive — **decision is
  the user's**.
- **FR-002**: The README MUST state what the app is, show current screenshots, and give build + install
  instructions; a `CONTRIBUTING` MUST describe how to build and contribute.
- **FR-003**: The dependency set MUST remain free of proprietary / Google-Play-Services components so
  the app qualifies for FOSS stores.
- **FR-004**: Each GitHub release MUST attach a developer-signed APK with notes; the repo MUST be
  Obtainium-compatible (releases discoverable).
- **FR-005**: Store metadata (title, descriptions, screenshots, changelog) MUST live in-repo under
  `fastlane/metadata/android` so stores can pull and update it.
- **FR-006**: The build MUST be made **reproducible** (deterministic APK) to satisfy IzzyOnDroid/F-Droid
  reproducible-build verification.
- **FR-007**: A **GitHub Pages** landing site MUST present the app with screenshots and live install
  links.
- **FR-008**: The app MUST be submitted to **IzzyOnDroid** (P2) and **F-Droid** (P3); Accrescent
  optional.
- **FR-009**: Google Play publication MUST be treated as a later, separate phase (account, AAB, listing,
  testing) and MUST NOT block the FOSS rollout.

### Key Entities

- **Release**: a git tag → signed APK + notes on GitHub Releases; the unit stores and Obtainium track.
- **Store metadata**: `fastlane/metadata/android/<locale>/` (title, short/full description, screenshots,
  changelogs) — single in-repo source for all stores.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can install the app from a GitHub release (directly or via Obtainium) in under
  2 minutes without a developer account or store.
- **SC-002**: The repo is public with a licence, screenshots, and build instructions a stranger can
  follow to a successful build.
- **SC-003**: The app is listed on at least one FOSS store (IzzyOnDroid first) and marked reproducible.
- **SC-004**: The landing page is live at a shareable URL with working install links.
- **SC-005**: No bundled dependency violates the chosen licence or FOSS-store policy (audited).

## Assumptions

- The `002` signed-release GitHub Actions workflow is reused/extended for tagged releases.
- `014-remove-pwa-traces` lands first (or alongside) so the public repo is native-only and not confusing.
- Screenshots can be generated from the app (light + dark) once the `010` redesign is in.
- The maintainer holds the signing key (already used by the release workflow) and a GitHub account; a
  Play developer account is not assumed until the later phase.
