# Feature Specification: Automated Dependency Updates

**Feature Branch**: `017-automated-deps`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the Gradle build, `gradle/libs.versions.toml` version
catalog, and the GitHub Actions CI that already runs test + Roborazzi verify + lint + assemble). Pairs
with `013-open-source-distribution` (a public repo benefits from visibly-maintained deps) and unblocks
`010`'s Compose-BOM bump for Expressive.

**Input**: "Research how to best keep deps up-to-date automatically, and spec." Research-first.

## Why this feature (context)

The app pins everything in a single version catalog (`gradle/libs.versions.toml`): AGP, Kotlin, the
Compose BOM, OkHttp, Coil, Roborazzi, etc. Today those drift by hand — which is how a project ends up
stuck on an old Compose BOM (exactly the blocker for `010`'s Expressive APIs) and how security fixes get
missed. This feature puts dependency updates on autopilot: a bot opens PRs for outdated libraries, the
Gradle wrapper, and plugins; the **existing CI gates every bump**; low-risk updates merge themselves and
risky ones wait for a human.

### Research findings

- **Renovate is the recommended tool** for this repo: it reliably reads/updates Gradle **version
  catalogs** (`libs.versions.toml`), updates the **Gradle wrapper** and **plugins**, **auto-rebases**
  the single-file conflicts that catalogs are prone to, and supports **grouping, scheduling, automerge,
  and a dependency dashboard**. Dependabot now supports version catalogs too but is weaker at Gradle and
  has no auto-rebase/grouping parity — it's the simpler GitHub-native fallback.
- **Coupling to respect**: with Kotlin 2.x the Compose compiler is the `org.jetbrains.kotlin.plugin.compose`
  plugin pinned to the **Kotlin** version — they MUST bump together. The **Compose BOM** governs
  `material3`/Compose artifact versions, so a BOM bump is the lever for the Expressive APIs and should be
  reviewed (it can change component behaviour/goldens).
- **CI as the gate**: because the repo already builds + tests + verifies Roborazzi on every push, a bad
  bump is caught before merge; automerge is only safe *because* of that gate.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Outdated deps show up as PRs automatically (Priority: P1)

When any catalog dependency, the Gradle wrapper, AGP, Kotlin, the Compose BOM, or a plugin has a newer
version, a bot opens a PR updating it — so I never have to hunt for versions by hand.

**Why this priority**: This is the feature; without automatic PRs nothing else matters.

**Acceptance Scenarios**:

1. **Given** an outdated `libs.versions.toml` entry, **When** the bot runs, **Then** a PR is opened that
   edits the catalog and passes (or fails) the existing CI.
2. **Given** an outdated Gradle wrapper or plugin, **When** the bot runs, **Then** it is updated by PR
   too.

### User Story 2 - Grouped & scheduled so it isn't noise (Priority: P2)

Related updates are grouped (Kotlin + the Compose compiler plugin together; AndroidX/Compose artifacts
together) and delivered on a schedule, so I get a few meaningful PRs, not dozens.

**Acceptance Scenarios**:

1. **Given** a Kotlin update, **When** the PR is opened, **Then** the coupled Compose-compiler plugin is
   bumped in the **same** PR.
2. **Given** several AndroidX updates, **When** the bot runs on its schedule, **Then** they arrive
   grouped rather than as one PR each.

### User Story 3 - Safe updates merge themselves (Priority: P2)

Low-risk updates (patch/minor of stable libraries) automerge once CI is green; high-risk ones (major
versions, AGP, Kotlin, the Compose BOM) wait for explicit human review.

**Acceptance Scenarios**:

1. **Given** a patch/minor update with green CI, **When** checks pass, **Then** it automerges without
   intervention.
2. **Given** a major / AGP / Kotlin / Compose-BOM update, **When** the PR opens, **Then** it is **not**
   automerged and is flagged for review.

### User Story 4 - Security updates are prioritised (Priority: P1)

A dependency with a known vulnerability gets an update PR promptly, ahead of the normal schedule.

**Acceptance Scenarios**:

1. **Given** a vulnerable dependency, **When** detected, **Then** a remediation PR is raised promptly and
   marked as security.

### Edge Cases

- A bump that breaks the build / changes Roborazzi goldens → CI fails the PR; it is **not** automerged;
  goldens are re-recorded as part of accepting it (ties to the redesign's golden workflow).
- The Kotlin/Compose-plugin pair updated out of step → grouping MUST prevent a mismatch that won't
  compile.
- Update-PR volume → scheduling + grouping + a dependency dashboard keep it to a reviewable cadence.
- A pinned version intentionally held back → the config MUST allow ignoring/pinning specific deps.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Adopt **Renovate** (recommended) configured for this repo; Dependabot is an acceptable
  fallback if Renovate can't be used. The choice and config live in-repo (e.g. `renovate.json`).
- **FR-002**: Automated update PRs MUST cover the **version catalog** (`libs.versions.toml`), the
  **Gradle wrapper**, **AGP**, **Kotlin**, the **Compose BOM**, and **Gradle plugins**.
- **FR-003**: Kotlin and the Compose compiler plugin MUST be updated **together** in one PR (version
  coupling); Compose/AndroidX artifacts SHOULD be grouped.
- **FR-004**: Every update PR MUST run the existing CI (test + Roborazzi verify + lint + assemble) and
  MUST NOT merge on red.
- **FR-005**: Low-risk updates (patch/minor of stable libs) MAY automerge on green CI; **major
  versions, AGP, Kotlin, and the Compose BOM MUST require manual review** (no automerge).
- **FR-006**: Security/vulnerability updates MUST be raised promptly, ahead of the normal schedule.
- **FR-007**: Updates MUST be **scheduled and grouped** to a reviewable cadence, with a **dependency
  dashboard** for visibility, and a way to **pin/ignore** specific dependencies.

### Key Entities

- **Bot config**: the in-repo Renovate/Dependabot configuration (schedule, grouping, automerge rules,
  ignore list).
- **Update PR**: a bot-authored change to the catalog/wrapper/plugins, gated by CI, tagged by risk
  (auto-mergeable vs review-required vs security).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A newly-released version of a tracked dependency results in an update PR within the
  configured schedule window (e.g. ≤1 week; security ≤1 day) with **no** manual action.
- **SC-002**: No update is ever merged with failing CI.
- **SC-003**: Kotlin/Compose-plugin updates never land mismatched (always co-bumped).
- **SC-004**: Routine low-risk updates require zero human action; risky ones always get human review.
- **SC-005**: Maintainer can see all pending updates at a glance (dependency dashboard) and pin/ignore
  any dep.

## Assumptions

- The existing CI is trustworthy enough to gate automerge (it builds, tests, verifies goldens) — this is
  the precondition that makes automation safe.
- The repo will be on GitHub (Renovate via the GitHub App or a self-hosted action; Dependabot native).
- Re-recording Roborazzi goldens for a bump that legitimately changes rendering is part of accepting that
  PR, not a reason to disable the bot.
- "Low-risk" = patch/minor of stable (non-AGP/Kotlin/BOM) libraries; the boundary can be tuned later.
