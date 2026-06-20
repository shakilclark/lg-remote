# Feature Specification: App Name Research (de-brand from "LG")

**Feature Branch**: `015-app-name`

**Created**: 2026-06-20

**Status**: Draft

**Relates to**: `013-open-source-distribution` (a public brand is needed before store/landing-page
submission) and `010-expressive-redesign` (the name feeds the icon/wordmark).

**Input**: "Research names for the app."

## Why this feature (context)

The working title is **"LG Remote"** — fine for a private build, but unusable for a public, store-listed
app: **"LG" is LG Electronics' trademark** and **"webOS" is also LG's**, so neither can headline an
independent app's name, store listing, or package id without inviting takedowns. Going open-source
(`013`) means the app needs its **own** brand: a name that's distinctive, available (store, package id,
domain, handle), trademark-clear, and a natural fit for an expressive TV-remote app. This feature is the
**research + decision**, not just a guess — it defines the criteria, the availability checks, and a
researched shortlist to choose from.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A shortlist that meets the constraints (Priority: P1)

I get a researched shortlist of candidate names, each checked against the hard constraints (no LG/webOS
trademark, no obvious app-store collision, plausible package id + domain + handle), so I can pick with
confidence rather than discover a conflict after launch.

**Why this priority**: A name that fails a trademark/availability check after publishing is expensive to
undo (re-list, re-id, re-art); the research must front-load those checks.

**Acceptance Scenarios**:

1. **Given** the shortlist, **When** I review each candidate, **Then** each lists its meaning/rationale
   and its check results (trademark sanity, Play/F-Droid/IzzyOnDroid name collision, `com.<x>` package
   id, domain, GitHub/social handle).
2. **Given** a chosen name, **When** I accept it, **Then** there are no known blocking conflicts and the
   package id + repo/brand can adopt it.

### User Story 2 - Clear naming criteria (Priority: P2)

The shortlist is generated against explicit criteria (short, memorable, pronounceable, evokes
remote/control/cast without "remote-control" genericness, not tied to LG, works as a wordmark with the
Ultraviolet brand) so the choice is principled.

**Acceptance Scenarios**:

1. **Given** the criteria, **When** candidates are proposed, **Then** each can be traced to the criteria
   and obviously-bad options (LG-derived, unpronounceable, conflicting) are excluded.

### Edge Cases

- A great name whose `.com`/`.app` domain is taken → note it; a domain isn't mandatory (GitHub Pages
  subdomain works), but record availability so the choice is informed.
- A name clashing with another remote/TV app on a store → exclude or flag clearly.
- Trademark checks here are **sanity checks, not legal advice**; flag anything that needs real
  diligence before commercial use.
- The package id (`applicationId`) ideally changes with the name, which affects signing/update identity
  on stores — note that renaming the id post-publish is disruptive, so decide before first public release.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Define explicit naming **criteria** (distinct from "LG"/"webOS"; short/memorable/
  pronounceable; evokes the product without being generic; wordmark-friendly with the Ultraviolet
  identity).
- **FR-002**: Produce a **researched shortlist** (≈5–8 candidates), each with rationale.
- **FR-003**: For each candidate, perform **availability/conflict checks**: trademark sanity, app-store
  name collision (Play/F-Droid/IzzyOnDroid), `com.<name>`-style package id plausibility, domain, and
  GitHub/social handle.
- **FR-004**: Exclude any candidate that uses or implies LG/webOS trademarks, or that collides with an
  existing TV/remote app.
- **FR-005**: Record a **recommendation** and leave the final pick to the user; the decision MUST be
  made before the first public release (`013`) and before the package id is finalised.
- **FR-006**: Note the downstream impacts of the chosen name: `applicationId`, repo name, app label,
  wordmark/icon, landing-page URL.

### Key Entities

- **Candidate name**: a proposed brand with {meaning, criteria-fit, trademark-sanity, store-collision,
  package-id, domain, handle} attributes.
- **Naming criteria**: the rubric every candidate is scored against.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A shortlist of ≥5 candidates exists, each with documented check results.
- **SC-002**: Zero shortlisted candidate uses "LG"/"webOS" or collides with a known TV/remote app.
- **SC-003**: The chosen name has a plausible, unclaimed package id and an available GitHub/handle path.
- **SC-004**: A single recommended name is decided and recorded before `013`'s first public release.

## Assumptions

- The app keeps describing *compatibility* with "LG webOS TVs" in prose/description (nominative fair
  use) while the **brand name itself** avoids the trademarks.
- Trademark checks are best-effort sanity checks (search-based), not a substitute for formal legal
  clearance if the project ever becomes commercial.
- A custom domain is optional (a GitHub Pages URL suffices for `013`); domain availability is recorded
  but not a blocker.
