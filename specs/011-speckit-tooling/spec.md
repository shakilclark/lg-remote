# Feature Specification: Spec Kit Tooling Adoption

**Feature Branch**: `011-speckit-tooling`

**Created**: 2026-06-20

**Status**: Draft

**Type**: Process / tooling (no app/runtime code; changes the `.specify/` workflow only)

**Input**: Strategic review of four spec-driven-development resources — `bigsmartben/spec-kit-workflow-preset`,
`rhuss/cc-spex`, and the Spec Kit community **extensions** and **presets** catalogs — evaluated against this
project's existing official Spec Kit setup. The recommendation: keep the official engine, add a small set of
low-risk extensions/presets, and graft the best review/visual-fidelity ideas by hand rather than installing a
second spec system. Full report artifact:
https://claude.ai/code/artifact/a91023c8-5f6b-4b5a-9289-e419d393ec9d

## Why this feature (context)

This repo runs the **official GitHub Spec Kit** (the `specify` CLI, `.specify/` with `constitution.md`,
templates, `speckit.*` commands, and the `agent-context` extension). Ten features are specced (`001`–`010`).
The workflow is working — so the bar for adding tooling is narrow: it must **cut ceremony** or **raise quality**
without **forking the engine**. Two evaluated repos (`cc-spex`, `spec-kit-workflow-preset`) fail that bar at
the package level (parallel ecosystem / wrong scale for a solo dev) but contain ideas worth grafting. The two
community catalogs contain a handful of clean, drop-in wins. This spec captures the adopt/steal/skip decision
so it can be implemented later as a deliberate change to the dev workflow, not ad hoc.

**Non-goals**: replacing the official Spec Kit engine; installing `cc-spex`; adopting the full
`workflow-preset` agent-handoff orchestration; re-namespacing any `speckit.*` command.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lightweight path for small changes (Priority: P1)

As the solo developer, when I make a small change (e.g. a haptics tweak or a reconnect fix), I want a
one-file spec path so I don't have to spin up a full `001`-style feature folder for trivial work.

**Why this priority**: This is the highest-frequency friction. Most future work is small, and full
ceremony per change is the main reason process gets skipped.

**Acceptance**: The **TinySpec** extension is installed; a single-file spec workflow is available and
documented in the repo; an existing-style feature folder is *not* required for a trivial change.

### User Story 2 - Better clarify + cheaper prompts (Priority: P1)

As the developer, I want `/speckit.clarify` to use Claude Code's native question UI, and I want the core
command prompts compacted to reduce token use — both with zero behaviour change to the spec outputs.

**Why this priority**: Pure quality-of-life and cost wins with negligible risk.

**Acceptance**: The **Claude AskUserQuestion** preset and the **Command Density** preset are installed and
registered; `/speckit.clarify` presents native questions; core prompts are measurably shorter.

### User Story 3 - Visual fidelity + accessibility for feature 010 (Priority: P2)

As the developer finishing the Expressive Redesign (`010`), I want to sign off the redesign visuals before
writing Compose code, and I want WCAG 2.2 AA accessibility baked into the remote's controls.

**Why this priority**: Directly serves the active redesign; high value but scoped to one feature, so it
sits below the always-on wins.

**Acceptance**: The **Wireframe Visual Feedback Loop** (or Interactive HTML Preview) extension is installed
and used for `010` sign-off; the **A11Y Governance** preset is installed; `010`'s checklist gains a
**visual-fidelity evidence matrix** (each redesign requirement linked to a screenshot at a stated fidelity
level L0–L3, an idea grafted from `spec-kit-workflow-preset`).

### User Story 4 - Review discipline grafted by hand (Priority: P3)

As the developer, I want the strongest ideas from `cc-spex` without installing it: a spec-approval gate
before `/speckit.implement`, and a `REVIEWERS.md`-style review guide per spec.

**Why this priority**: Valuable discipline, but achievable with the existing `/code-review` skill plus
light convention; lowest urgency and no new dependency.

**Acceptance**: A documented convention exists for (a) approving a spec before implementation begins and
(b) a short per-spec review guide; no `cc-spex` package or `speckit-spex-*` namespace is added.

## Decision summary (adopt / steal / skip)

| Resource | Verdict | Action |
|---|---|---|
| Extensions catalog — TinySpec, Brownfield Bootstrap/Time Machine, Wireframe Loop, Bugfix Workflow, Checkpoint | **Adopt selectively** | Install TinySpec + Wireframe Loop now; Brownfield/Time Machine, Bugfix, Checkpoint as needed |
| Presets catalog — Claude AskUserQuestion, Command Density, A11Y Governance | **Adopt selectively** | Install all three |
| `spec-kit-workflow-preset` (bigsmartben) | **Steal one idea** | Graft the visual-fidelity evidence matrix into `010`; skip the agent-handoff orchestration (wrong scale for solo) |
| `cc-spex` (rhuss) | **Steal the ideas** | Graft spec-approval gate + `REVIEWERS.md`; do **not** install (forks the engine; team-shaped; `/code-review` covers deep review) |

## Implementation notes (for later)

- Community extensions/presets are **author-maintained and not audited** by Spec Kit — review each before install.
- Install order suggestion: P1 items first (TinySpec, AskUserQuestion, Command Density), verify each command
  registers, then the `010`-scoped P2 items, then the P3 conventions.
- Verify after each install that existing `speckit.*` commands and the `agent-context` extension still work.

## Success Criteria

- **SC-1**: A trivial change can be specced via a single-file path without a full feature folder.
- **SC-2**: `/speckit.clarify` uses native questions and core prompts are shorter, with identical spec output.
- **SC-3**: Feature `010` ships with a visual-fidelity evidence matrix and WCAG 2.2 AA checks in its checklist.
- **SC-4**: A spec-approval gate and per-spec review guide convention are documented and in use.
- **SC-5**: No second spec engine is installed; the official Spec Kit setup remains the single source of truth.
