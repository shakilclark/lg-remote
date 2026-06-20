# Specification Quality Checklist: Activate Inactive UI Shells

**Created**: 2026-06-20
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Coordination/backlog framing is clear; each shell mapped to mechanism + owning spec
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] Requirements testable (inactive-distinguishable, layout-stable on activation, honest feedback)
- [x] Success criteria measurable (0 fake-live controls; every shell has a path; backlog → empty)
- [x] Scope bounded (wiring backlog; the rebuild owns the layout)
- [x] Dependencies identified (006/009/016 + new voice/WoL/keyboard specs)

## Notes

- Key rule (FR-001/FR-004): an inactive shell must look inactive and give honest "not yet" feedback —
  never fake success. Activation must not reflow the layout (FR-002).
