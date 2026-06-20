# Specification Quality Checklist: Trackpad Back Control — Design Options

**Created**: 2026-06-20
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Focused on the user problem (fast + findable Back) and the conflict constraint
- [x] Written for non-technical stakeholders; options framed for a decision
- [x] All mandatory sections completed

## Requirement Completeness

- [x] Requirements testable (no-conflict, visible affordance, taught, gesture-distinguished)
- [x] Success criteria measurable (first-session success, 0 false positives, ≥95% recognition)
- [x] Edge cases (pinch confusion, finger-lift, a11y) identified
- [x] Options rated on conflict / discoverability / speed
- [x] Recommendation stated; final pick left to the user

## Notes

- Core principle (from research): never a hidden gesture alone — touchpad-only remotes that hide Back
  (Siri Remote) fail. Recommend two-finger tap (A) + a persistent visible Back (C) + command-sheet
  fallback, taught by the first-run card.
