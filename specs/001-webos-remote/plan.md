# Implementation Plan: LG webOS TV Remote (App-like Phone Remote)

**Branch**: `001-webos-remote` | **Date**: 2026-06-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-webos-remote/spec.md`

## Summary

Build a touch-first remote for an LG webOS TV that installs to the home screen on the
user's Android phone and iPad. The browser cannot open the TV's insecure local WebSocket
from a secure/installed app, so a small **Node.js + TypeScript backend on the user's
always-on Raspberry Pi** owns the connection to the TV (server→TV is unrestricted),
persists the pairing key, and exposes a clean local API + live state stream to the UI. The
**PWA frontend (React + TypeScript + Vite)** talks only to that backend. **Tailscale Serve**
fronts the backend with a valid HTTPS certificate, which (a) makes the PWA genuinely
installable on both devices with zero certificate fuss and (b) extends control beyond the
home network — all over the user's own private overlay, with no third-party control cloud.

MVP scope = User Stories 1–3 (pairing, volume/mute, play/pause, menu navigation). App
launcher + input switching (US4, FR-007/008) are deferred to a follow-up slice.

## Technical Context

**Language/Version**: TypeScript 5.x on Node.js 20 LTS (backend); TypeScript 5.x + React 18 (frontend)

**Primary Dependencies**:
- Backend: `lgtv2` (webOS SSAP client: pairing, client-key persistence, auto-reconnect, pointer-input socket for buttons), `node-ssdp` (discovery), `express` (static + REST), `ws` (state push to UI), `zod` (input validation)
- Frontend: `react`, `react-dom`, `vite`, `vite-plugin-pwa` (manifest + service worker)

**Storage**: Single JSON file on the Raspberry Pi (`~/.config/lg-remote/store.json`) holding remembered TV connections (name, last-known address, pairing client-key). No database, no accounts.

**Testing**: `vitest` for backend unit/contract tests against a **mock webOS WebSocket server** that speaks the SSAP handshake; manual end-to-end verification against the real TV via `quickstart.md`. `vitest` + Testing Library for frontend component/state tests.

**Target Platform**: Backend runs on Linux (Raspberry Pi), also runnable on macOS for dev. Frontend runs as an installed PWA on current Android Chrome and iPadOS Safari.

**Project Type**: Web application (separate `backend/` + `frontend/`).

**Performance Goals**: Control round-trip < 1s on LAN (SC-003); auto-reconnect to a known TV < 5s when reachable (SC-002).

**Constraints**: No third-party control cloud (FR-013); control traffic stays on the user's LAN / Tailscale overlay. Touch-first, one-handed portrait UI (FR-012). Must fail visibly when the TV is unreachable (FR-014).

**Scale/Scope**: Single user, a small number of remembered TVs (1 typical). ~6–8 control actions in MVP. Not a multi-tenant service.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Zero-Fuss Operation | Pairing happens once; key persisted server-side and reused for all the user's devices; auto-reconnect. The one-time Tailscale install is **admin setup on the Raspberry Pi**, never in the phone user flow. | ✅ PASS |
| II. Installable & Phone-First | PWA installs to home screen on Android + iPad; UI is touch-first, portrait, one-handed. | ✅ PASS |
| III. Local-Network & Private by Default | Control goes phone → Raspberry Pi → TV over LAN/Tailscale; the pairing key lives only on the user's Raspberry Pi; no third-party service processes control traffic or usage data. (Tailscale's coordination plane sees connection metadata only, not SSAP traffic — noted, acceptable.) | ✅ PASS |
| IV. Resilient Connectivity | `lgtv2` auto-reconnects and emits connect/close; backend maps these to an explicit state machine (needs-pairing/connecting/connected/disconnected/off-network) pushed live to the UI. | ✅ PASS |
| V. Demonstrable Vertical Slices | Slices: (1) pair+connect, (2) volume/playback, (3) navigation — each independently demoable on the phone against the real TV. | ✅ PASS |

No violations → Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-webos-remote/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── backend-api.md    # REST + WS contract the frontend depends on
│   └── tv-protocol.md    # SSAP/webOS commands the backend depends on
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
backend/
├── src/
│   ├── tv/                 # webOS integration
│   │   ├── client.ts        # lgtv2 wrapper: connect, pair, reconnect, button socket
│   │   ├── commands.ts      # SSAP command map (volume/mute/play-pause/nav)
│   │   └── discovery.ts     # SSDP scan for LG TVs
│   ├── state/
│   │   └── connection.ts    # connection-state machine
│   ├── store/
│   │   └── store.ts         # JSON persistence of TV connections + keys
│   ├── api/
│   │   ├── rest.ts          # Express routes (discover, pair, command, state)
│   │   └── events.ts        # ws push of live state to the UI
│   └── server.ts            # wires it together; serves built frontend
└── tests/
    ├── mock-tv.ts           # mock webOS WebSocket server (SSAP handshake + commands)
    ├── client.test.ts
    ├── commands.test.ts
    └── api.test.ts

frontend/
├── src/
│   ├── api/client.ts        # talks to backend REST + WS
│   ├── state/useConnection.ts
│   ├── components/          # ConnectionBanner, VolumePad, PlaybackBar, DPad, ...
│   ├── App.tsx
│   └── main.tsx
├── public/                  # PWA icons
├── index.html
├── vite.config.ts           # vite-plugin-pwa (manifest + SW)
└── tests/

docs/
└── setup-tailscale.md       # one-time Raspberry Pi HTTPS/remote-access setup
```

**Structure Decision**: Web application (Option 2). The backend/frontend split is forced by
the architecture: the backend exists precisely to hold the TV socket the browser can't. The
backend additionally serves the built frontend so there is a single thing to run on the
Raspberry Pi.

## Complexity Tracking

> No constitution violations — no entries required.
