# Specification Quality Checklist: Remove Traces of the PWA

**Created**: 2026-06-20
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Focused on outcome (native-only repo) and the one risk (preserve protocol knowledge)
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] Requirements testable and unambiguous
- [x] Success criteria measurable (grep-clean, footprint drop, CI passes, links resolve)
- [x] Acceptance scenarios + edge cases (CI refs, git history, archived 001) identified
- [x] Scope bounded (delete-forward, not history rewrite; 001 archived not deleted)
- [x] Dependencies/assumptions identified

## Notes

- The binding constraint is FR-003/FR-004: `tv-protocol.md` and the `001` spec are **preserved**
  (archived), because `CLAUDE.md` and specs 002/004/009 depend on them. Everything else (frontend,
  backend, mockups, Tailscale doc) is removed.
