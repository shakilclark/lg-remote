# Specification Quality Checklist: Open Source & Distribution

**Created**: 2026-06-20
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Focused on user/maintainer value (install paths, openness), not implementation detail
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] Requirements testable and unambiguous
- [x] Success criteria measurable and technology-agnostic
- [x] Acceptance scenarios + edge cases (non-reproducible, sideload verification, licence/dep conflict)
- [x] Scope bounded (FOSS rollout now; Play deferred)
- [x] Dependencies/assumptions identified (002 release workflow, 014 first, signing key)

## Open decision (not blocking)

- **Licence choice** is explicitly the user's call (FR-001). Recommendation: GPL-3.0-or-later
  (F-Droid-native copyleft, prevents proprietary appropriation). Permissive (Apache-2.0/MIT) is fine if
  the user prefers maximum reuse. Resolve before publishing.

## Notes

- Research-first per request; findings (IzzyOnDroid lowest-friction, reproducible builds, fastlane
  metadata, F-Droid policy, Sept-2026 sideload-verification risk) are summarised in the spec context.
