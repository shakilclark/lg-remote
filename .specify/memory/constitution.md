<!--
Sync Impact Report
==================
Version change: 1.0.0 → 1.1.0
Bump rationale: Architecture pivot to a native Android app talking directly to the TV over the
  LAN (no backend, no Tailscale, no HTTPS). Principles II–III and the Platform & Connectivity
  constraints are materially re-scoped (Android-only, home-network-only, native cert-trust
  instead of the browser-security workaround). The five principles' intent is preserved, so this
  is a MINOR amendment, not a redefinition that removes any principle.
Modified principles:
  - II. Installable & Phone-First → installable = native Android APK (sideloaded), not a PWA.
  - III. Local-Network & Private by Default → control is now device→TV direct; no server tier.
Added sections: none (Platform & Connectivity constraints rewritten in place)
Removed sections: none
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ reviewed (Constitution Check gate still compatible)
  - .specify/templates/spec-template.md ✅ reviewed (no mandatory-section conflicts)
  - .specify/templates/tasks-template.md ✅ reviewed (task categories compatible)
Deferred TODOs: none
-->

# LG Remote Constitution

## Core Principles

### I. Zero-Fuss Operation (NON-NEGOTIABLE)

The end-to-end experience MUST be optimised for the least possible user effort. Pairing
with a TV happens once and is then remembered. Launching the remote on the phone MUST take
no more taps than opening any native app. Setup steps that demand terminal commands or
per-session configuration on the *phone* are forbidden in the primary user flow. (Trusting
the TV's self-signed certificate is handled silently in code, never asked of the user.)
Rationale: this is a personal convenience tool; if it is harder than the physical remote,
it has failed.

### II. Installable & Phone-First

The remote MUST be a real installed Android app launched from the home screen, behaving as
a native app (no browser, no PWA shell). The UI MUST be designed touch-first for one-handed
phone use in portrait orientation. Distribution is by sideloading a signed APK (e.g. `adb
install`); an app-store account MUST NOT be a prerequisite. Rationale: the user explicitly
wants a real, installed phone app, and a native build is what lets the app talk directly to
the TV without a server.

### III. Local-Network & Private by Default

All TV control happens directly between the phone and the TV over the user's local network —
there is NO backend, relay, or cloud service in the control path. The app MUST NOT route
control traffic, the TV's client-key, or usage data through any third party. The TV's
pairing key is a credential and MUST be stored only in the app's private on-device storage.
Rationale: a TV remote should not phone home; direct local control is faster, more private,
and removes the always-on server the previous architecture required.

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
- Target client: a native **Android** app (Kotlin + Jetpack Compose). iOS/iPadOS and
  desktop are explicitly out of scope for this architecture.
- Connectivity scope: **home Wi-Fi / same-LAN only**. Away-from-home control is out of
  scope (it was what required the previous server + tunnel).
- TV transport: connect to the TV's secure SSAP socket (`wss://<tv-ip>:3001`); the TV's
  self-signed certificate MUST be trusted programmatically (custom `TrustManager`) since
  the native app is not bound by a browser's certificate rules. Deprecated cleartext
  `ws://:3000` MUST NOT be relied upon.
- Android platform: must function on modern Android; where the OS requires it (Android 17 /
  targetSDK 37+), the app MUST request the runtime local-network permission and degrade
  gracefully if denied (per Principle IV).
- No account system, no user database. The only persisted state is TV connection details
  and the pairing key, in the app's private storage.

## Development Workflow

- Spec-Driven Development is the workflow: specify → (clarify) → plan → tasks →
  (analyze) → implement. Code follows an approved spec and plan.
- Each completed feature slice is committed with a clear, semantic message; the working
  tree is kept runnable at each commit.
- Manual verification on the Android phone (or documented simulation) is required before a
  slice is considered complete, per Principle V.

## Governance

This constitution supersedes ad-hoc preferences during the project. Amendments are made
by updating this file with a version bump and a Sync Impact Report, and by re-checking the
dependent Spec Kit templates for alignment. Plans and implementations MUST be checked
against these principles at the Constitution Check gate; any deviation MUST be justified
in writing in the plan's Complexity Tracking section. Versioning follows semantic rules:
MAJOR for principle removals/redefinitions, MINOR for added principles or materially
expanded guidance, PATCH for clarifications.

**Version**: 1.1.0 | **Ratified**: 2026-06-18 | **Last Amended**: 2026-06-18
