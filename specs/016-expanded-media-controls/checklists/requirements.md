# Specification Quality Checklist: Expanded Media Controls in Now-Playing

**Created**: 2026-06-20
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Focused on viewer value (captions first) and honesty about best-effort controls
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] Requirements testable and unambiguous
- [x] Success criteria measurable
- [x] Acceptance scenarios + edge cases (Live-TV vs app, ignored buttons, rapid taps)
- [x] Scope bounded (CC + skip + overflow; no scrubber; no per-app capability probing)
- [x] Dependencies/assumptions identified (reuses pointer-input socket; 004 honesty stance)

## Notes

- Binding honesty constraint (FR-005): button injection is fire-and-forget, so no expanded control may
  show a confirmed state webOS doesn't report (captions = toggle *request*, not "ON"). Primary
  transport row stays minimal (FR-003/FR-007); extras live in a "more" overflow.
