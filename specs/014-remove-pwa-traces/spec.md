# Feature Specification: Remove Traces of the PWA

**Feature Branch**: `014-remove-pwa-traces`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the native re-platform). Pairs with
`013-open-source-distribution` (a clean, native-only repo before going public).

**Input**: "Remove traces of the PWA."

## Why this feature (context)

The app was re-platformed from a PWA (browser app + Node backend + Tailscale tunnel) to a native
Android app that talks **directly** to the TV over the LAN. The old PWA stack is still sitting in the
repository — `frontend/` (~64 MB, Vite + `public/sw.js` service worker), `backend/` (~81 MB, Node +
tests), `mockups/`, `docs/setup-tailscale.md`, and the PWA-era spec `specs/001-webos-remote/` — even
though the **constitution now forbids a backend/Tailscale** (Principle III). This dead weight bloats
the repo, confuses newcomers, and directly contradicts the current architecture — a problem before
open-sourcing (`013`).

**Critical constraint**: `CLAUDE.md` names `specs/001-webos-remote/contracts/tv-protocol.md` (and the
`backend/` SSAP code) as the **protocol source of truth**. The Kotlin SSAP client was written from
scratch, so the *code* is disposable, but the **protocol knowledge MUST be preserved**.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The repo is native-only (Priority: P1)

A newcomer cloning the repo sees only the native Android app and its specs/docs — no Vite frontend, no
Node backend, no service worker, no Tailscale — so what's there matches what the app actually is.

**Why this priority**: The contradiction (dead PWA vs native-only constitution) is the whole point; it
must go before the repo is public.

**Acceptance Scenarios**:

1. **Given** the repo, **When** I list the top level, **Then** `frontend/`, `backend/`, and `mockups/`
   are gone and only the native app (`android/`), `docs/`, and `specs/` remain.
2. **Given** a search for `tailscale`, `service-worker`/`sw.js`, `vite`, or the Node backend, **Then**
   no live source or build config matches (only historical mentions in archived specs, clearly marked).

### User Story 2 - Protocol knowledge is preserved (Priority: P1)

The SSAP protocol reference that the native client depends on survives the cleanup — relocated to a
durable home and still linked from `CLAUDE.md`/specs.

**Acceptance Scenarios**:

1. **Given** the removal, **When** I look for the SSAP protocol contract, **Then** `tv-protocol.md`
   still exists (kept in an archived `001` or moved under `docs/`) and is referenced by `CLAUDE.md`.
2. **Given** specs `002`/`004`/`009` that cite `001` as prior art, **When** I follow those links,
   **Then** they still resolve (001 retained as an archived/superseded spec, not deleted).

### User Story 3 - Docs reflect native-only (Priority: P2)

`README.md` and `CLAUDE.md` no longer describe a PWA/backend/Tailscale setup; they describe the native
app, its build, and (post-`013`) install paths.

**Acceptance Scenarios**:

1. **Given** the README, **When** I read it, **Then** there are no PWA/backend/Tailscale setup
   instructions; it documents the native app.
2. **Given** `CLAUDE.md`, **When** I read the architecture/prior-art notes, **Then** the backend "source
   of truth" pointer is updated to the relocated protocol doc.

### Edge Cases

- A build/CI/config file referencing the removed dirs → CI MUST still pass after removal (no dangling
  paths in workflows, gitignore, root tooling).
- Git history → removal is by deletion in a normal commit (history retains the PWA); a full history
  rewrite is out of scope unless the user asks.
- The `001` spec is **archived, not deleted** (it documents the protocol + the project's origin), with
  a clear "superseded by 002 (native re-platform)" banner.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Remove the PWA implementation from the repo: `frontend/` (incl. `public/sw.js`),
  `backend/`, and `mockups/`.
- **FR-002**: Remove PWA-era operational docs that no longer apply, specifically `docs/setup-tailscale.md`.
- **FR-003**: Preserve the SSAP protocol reference — keep `tv-protocol.md` (retain `specs/001` as an
  archived spec, or relocate the contract under `docs/`) and keep `CLAUDE.md`/spec links to it valid.
- **FR-004**: Mark `specs/001-webos-remote` as **archived/superseded by 002** with a short banner;
  do not delete it (it carries the protocol + project origin and is cited by later specs).
- **FR-005**: Update `README.md` and `CLAUDE.md` to remove PWA/backend/Tailscale setup content and
  reflect the native-only architecture.
- **FR-006**: After removal, the Android build + CI MUST be unaffected (no references to removed paths
  in workflows, root configs, or `.gitignore` that break).

### Key Entities

- **PWA stack**: `frontend/`, `backend/`, `mockups/`, `setup-tailscale.md` — the artefacts to remove.
- **Protocol reference**: `tv-protocol.md` — the one thing inside the old material that must survive.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The repo working tree contains no live PWA/backend/Tailscale source or build config; a
  grep for `tailscale`/`sw.js`/`vite`/the Node backend matches only clearly-archived spec text.
- **SC-002**: Repo footprint drops materially (≈145 MB of `frontend/`+`backend/` removed from the tree).
- **SC-003**: The SSAP protocol contract is still present and linked; specs `002`/`004`/`009` prior-art
  links to `001` still resolve.
- **SC-004**: Android build + CI pass unchanged after the removal.
- **SC-005**: README/CLAUDE.md describe only the native app — a new reader is not misled about the
  architecture.

## Assumptions

- The native Kotlin SSAP client fully replaces the Node backend (per `002`); none of the app's runtime
  depends on `frontend/` or `backend/`.
- History rewriting (purging the PWA from past commits) is out of scope unless explicitly requested;
  deletion in a forward commit is sufficient.
- `001` is kept as an archived spec rather than deleted, to preserve protocol + provenance.
