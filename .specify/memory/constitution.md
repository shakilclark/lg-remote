<!--
Sync Impact Report
==================
Version change: (template) → 1.0.0
Bump rationale: Initial ratification of the project constitution (template placeholders → concrete principles).
Modified principles: n/a (initial adoption)
Added sections:
  - Core Principles (I–V)
  - Additional Constraints (Platform & Connectivity)
  - Development Workflow
  - Governance
Removed sections: none
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ reviewed (Constitution Check gate compatible)
  - .specify/templates/spec-template.md ✅ reviewed (no mandatory-section conflicts)
  - .specify/templates/tasks-template.md ✅ reviewed (task categories compatible)
Deferred TODOs: none
-->

# LG Remote Constitution

## Core Principles

### I. Zero-Fuss Operation (NON-NEGOTIABLE)

The end-to-end experience MUST be optimised for the least possible user effort. Pairing
with a TV happens once and is then remembered. Launching the remote on a phone MUST take
no more taps than opening a native app. Setup steps that demand terminal commands,
certificate juggling, or per-session configuration on the *phone* are forbidden in the
primary user flow. Rationale: this is a personal convenience tool; if it is harder than
the physical remote, it has failed.

### II. Installable & Phone-First

The remote MUST be installable to a phone home screen and launch standalone (no browser
chrome), behaving like a native app. The UI MUST be designed touch-first for one-handed
phone use in portrait orientation; desktop is a bonus, never the priority. Rationale: the
user explicitly wants a real, installed phone app without the fuss of app-store
distribution.

### III. Local-Network & Private by Default

All TV control happens over the user's local network. The app MUST NOT route control
traffic, the TV's client-key, or usage data through any third-party/cloud service. The
TV's pairing key is a credential and MUST be stored only where the user controls it.
Rationale: a TV remote should not phone home, and local control is both faster and more
private.

### IV. Resilient Connectivity

The app MUST treat the TV connection as unreliable: it can be off, asleep, on a changed
IP, or mid-reconnect. Every control action MUST give the user clear feedback on
connection state (connected / connecting / disconnected / needs-pairing) and MUST fail
visibly rather than silently. Reconnection SHOULD be automatic where possible.
Rationale: TVs power off and DHCP leases change; a remote that lies about its state is
worse than no remote.

### V. Demonstrable Vertical Slices

Work MUST be sliced so that each delivered increment is independently usable and testable
end-to-end (e.g. "pair with TV", then "volume control", then "app launcher"). A slice is
not done until it can be demonstrated on a real phone against a real TV (or a documented
mock when no TV is present). Rationale: this project exists to exercise Spec-Driven
Development; prioritised, independently-shippable slices are the unit of progress.

## Additional Constraints: Platform & Connectivity

- Target TV: LG televisions running webOS, controlled via their documented local
  WebSocket control protocol (SSAP). Other TV brands are out of scope.
- The browser security model (an installed/HTTPS app cannot open insecure connections to
  the TV) is a known, central constraint. The chosen architecture MUST resolve this
  explicitly rather than ignore it; the resolution is decided at the planning phase.
- Minimum supported phone browsers: current Safari (iOS) and Chrome (Android).
- No account system, no user database. The only persisted state is TV connection details
  and the pairing key.

## Development Workflow

- Spec-Driven Development is the workflow: specify → (clarify) → plan → tasks →
  (analyze) → implement. Code follows an approved spec and plan.
- Each completed feature slice is committed with a clear, semantic message; the working
  tree is kept runnable at each commit.
- Manual verification on a phone (or documented simulation) is required before a slice is
  considered complete, per Principle V.

## Governance

This constitution supersedes ad-hoc preferences during the project. Amendments are made
by updating this file with a version bump and a Sync Impact Report, and by re-checking the
dependent Spec Kit templates for alignment. Plans and implementations MUST be checked
against these principles at the Constitution Check gate; any deviation MUST be justified
in writing in the plan's Complexity Tracking section. Versioning follows semantic rules:
MAJOR for principle removals/redefinitions, MINOR for added principles or materially
expanded guidance, PATCH for clarifications.

**Version**: 1.0.0 | **Ratified**: 2026-06-18 | **Last Amended**: 2026-06-18
