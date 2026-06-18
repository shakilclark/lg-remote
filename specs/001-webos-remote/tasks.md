---
description: "Task list for LG webOS TV Remote (001-webos-remote)"
---

# Tasks: LG webOS TV Remote (App-like Phone Remote)

**Input**: Design documents from `specs/001-webos-remote/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included — the plan (research R8) commits to a mock-TV + `vitest` test layer, and
Principle V requires demonstrable slices. Test tasks are scoped and precede implementation
within each story.

**Organization**: Grouped by user story (US1–US3 = MVP). US4 (app launcher/inputs) is out
of MVP scope and listed under Deferred.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- File paths are relative to repo root (`backend/`, `frontend/`).

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Create repo structure per plan: `backend/`, `frontend/`, `docs/` with `backend/src/{tv,state,store,api}`, `backend/tests`, `frontend/src/{api,state,components}`.
- [X] T002 Initialize backend Node+TS project in `backend/`: `package.json`, `tsconfig.json`, deps `lgtv2 node-ssdp express ws zod`, devDeps `typescript tsx vitest @types/express @types/ws @types/node`; scripts `dev`/`build`/`start`/`test`.
- [X] T003 [P] Initialize frontend Vite+React+TS project in `frontend/` with `vite-plugin-pwa`; scripts `dev`/`build`/`test`; Vite dev proxy `/api` → `http://localhost:8080`; build `outDir` → `../backend/public`.
- [X] T004 [P] Configure shared lint/format (ESLint + Prettier) and an `.editorconfig` at repo root.
- [X] T005 [P] Add root `README.md` and `.gitignore` (node_modules, `backend/public`, `~/.config` not in repo, build output).

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: No user-story work begins until this phase is complete.

- [X] T006 [P] Define shared types in `backend/src/types.ts`: `TVConnection`, `ConnectionStatus`, `ConnectionState`, `ControlCommand` (mirrors data-model.md).
- [X] T007 [P] Implement JSON store in `backend/src/store/store.ts`: read/write `~/.config/lg-remote/store.json`, CRUD for TVs, get/set `activeId`; never expose `clientKey` outside the module.
- [X] T008 Implement connection-state machine in `backend/src/state/connection.ts`: statuses needs-pairing/connecting/connected/disconnected/off-network + an event emitter for state changes (data-model.md transitions).
- [X] T009 Implement Express app skeleton + WS server in `backend/src/api/rest.ts`, `backend/src/api/events.ts`, `backend/src/server.ts`: serve `backend/public`, mount `/api`, error-handling middleware returning `{ message }`, `/api/events` WS broadcasting `ConnectionState`.
- [X] T010 [P] Create the mock webOS server in `backend/tests/mock-tv.ts`: a `ws` server implementing the SSAP register/`client-key` handshake, `prompt` flow, and ack of command URIs + pointer-input socket (contracts/tv-protocol.md).
- [X] T011 [P] Wire `vitest` config in `backend/` and a smoke test that boots the server and hits `GET /api/state`.

**Checkpoint**: Server boots, serves UI shell, pushes state over WS, tests run against mock.

---

## Phase 3: User Story 1 — Pair with my TV once (Priority: P1) 🎯 MVP

**Goal**: Discover/point at the TV, complete one-time pairing, persist the key, auto-reconnect.

**Independent Test**: Fresh device → connect → approve prompt on TV → "connected"; reopen → reconnects with no prompt (quickstart US1).

### Tests for US1

- [X] T012 [P] [US1] Contract test `backend/tests/api.pair.test.ts`: `POST /api/discover`, `POST /api/tvs`, `POST /api/pair` against mock-tv produce needs-pairing → connected and persist a key.
- [X] T013 [P] [US1] Unit test `backend/tests/client.test.ts`: lgtv2 wrapper maps `prompt`/`connect`/`close` events to ConnectionState transitions.

### Implementation for US1

- [X] T014 [P] [US1] Implement SSDP discovery in `backend/src/tv/discovery.ts` (scan + filter LG; return candidates).
- [X] T015 [US1] Implement lgtv2 wrapper in `backend/src/tv/client.ts`: connect to `ws://<ip>:3000`, pass `clientKey`/`saveKey` to the store, `reconnect: 5000`, emit state via the state machine (T008), expose `connect()/disconnect()`.
- [X] T016 [US1] Implement REST routes in `backend/src/api/rest.ts`: `GET /api/tvs`, `POST /api/discover`, `POST /api/tvs`, `POST /api/pair`, `GET /api/state` (contracts/backend-api.md); validate bodies with zod.
- [X] T017 [US1] On server boot, auto-load active TV from store and begin connect/reconnect; broadcast state to `/api/events`.
- [X] T018 [P] [US1] Frontend backend client in `frontend/src/api/client.ts` + `frontend/src/state/useConnection.ts`: subscribe to `/api/events`, expose discover/addTV/pair calls and live state.
- [X] T019 [US1] Frontend onboarding UI: `frontend/src/components/ConnectScreen.tsx` (discover list + manual IP entry) and `frontend/src/components/ConnectionBanner.tsx` (always-visible status); shown until connected.

**Checkpoint**: Pairing works end-to-end on the real TV; reopen reconnects silently.

---

## Phase 4: User Story 2 — Control playback and volume (Priority: P1) 🎯 MVP

**Goal**: Volume up/down, mute (with state), play/pause via large touch buttons.

**Independent Test**: Connected TV → volume/mute/play-pause respond within ~1s; app reflects volume/mute (quickstart US2).

### Tests for US2

- [ ] T020 [P] [US2] Contract test `backend/tests/api.command.test.ts`: `POST /api/command` for `volumeUp`/`volumeDown`/`setMute`/`playPause` → `acknowledged`; rejects when disconnected (409) and unknown type (422).

### Implementation for US2

- [ ] T021 [P] [US2] Implement SSAP command map in `backend/src/tv/commands.ts`: volumeUp/Down, setMute, play/pause; subscribe to `ssap://audio/getVolume` to keep `volume`/`muted` in ConnectionState.
- [ ] T022 [US2] Add `POST /api/command` route (volume/mute/playPause) in `backend/src/api/rest.ts` with zod validation + connection-state guard (FR-014).
- [ ] T023 [US2] Track last media state in `backend/src/tv/commands.ts` so `playPause` toggles correctly (contracts/tv-protocol.md note).
- [ ] T024 [P] [US2] Frontend `frontend/src/components/VolumePad.tsx` (+/−, mute with live level) and `frontend/src/components/PlaybackBar.tsx` (play/pause), wired to `POST /api/command`; large touch targets, portrait (FR-012).

**Checkpoint**: Everyday remote (volume + playback) usable on the phone against the real TV — shippable MVP if stopped here.

---

## Phase 5: User Story 3 — Navigate the TV menus (Priority: P2)

**Goal**: D-pad (up/down/left/right), OK, back, home over the pointer-input socket.

**Independent Test**: Connected TV → directions move focus, OK selects, back/home navigate (quickstart US3).

### Tests for US3

- [ ] T025 [P] [US3] Unit test `backend/tests/nav.test.ts`: `nav` commands acquire the pointer-input socket once and send the right button names to mock-tv; invalid button → 422.

### Implementation for US3

- [ ] T026 [US3] Extend `backend/src/tv/client.ts`/`commands.ts`: acquire pointer-input socket via `getPointerInputSocket`, cache it, expose `button(name)` (UP/DOWN/LEFT/RIGHT/ENTER/BACK/HOME/EXIT).
- [ ] T027 [US3] Extend `POST /api/command` to handle `{ type: "nav", params: { button } }` with zod enum validation.
- [ ] T028 [P] [US3] Frontend `frontend/src/components/DPad.tsx` (directional pad + OK center, Back, Home) wired to `nav` commands; thumb-reachable layout.

**Checkpoint**: All MVP stories (US1–US3) independently functional on the real TV.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T029 [P] PWA manifest + icons + service worker via `vite-plugin-pwa` in `frontend/vite.config.ts` and `frontend/public/`; standalone display, portrait, app name/theme (FR-011, SC-004).
- [ ] T030 [P] App shell/layout `frontend/src/App.tsx`: compose ConnectionBanner + VolumePad + PlaybackBar + DPad; offline/disconnected affordances (FR-009/014).
- [ ] T031 [P] Off-network detection in `backend/src/state/connection.ts` (no route/ping fail → off-network) and surfaced in UI (edge cases, SC-005).
- [X] T032 [P] Write `docs/setup-tailscale.md`: install Tailscale on Raspberry Pi/phone/iPad, `tailscale serve --bg 8080`, install PWA from the `*.ts.net` URL (SC-004, SC-007).
- [ ] T033 [P] Production wiring: `frontend` build → `backend/public`; `backend` `npm start` serves both; document in `README.md`.
- [ ] T034 Run `quickstart.md` validation end-to-end on the real TV from the Android phone and iPad; record results.

---

## Deferred (post-MVP — User Story 4, P3)

- [ ] D001 [US4] App launcher: `listLaunchPoints` + `system.launcher/launch` (FR-007) + UI grid.
- [ ] D002 [US4] Input switching: `getExternalInputList` + `switchInput` (FR-008) + UI list.

---

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2)** blocks everything.
- **US1 (P3 phase)** must precede US2/US3 in practice (they need a live connection), though each story is independently *testable* against mock-tv.
- **US2** and **US3** are independent of each other once US1's client exists; their backend command work (T021/T026) and frontend (T024/T028) can parallelize.
- **Polish (P6)** after the desired stories.

### Parallel opportunities
- Setup: T003/T004/T005 in parallel.
- Foundational: T006/T007 and T010/T011 in parallel.
- Within stories: test tasks [P] + frontend components [P] parallel to backend command work.

---

## Implementation Strategy

### MVP (stop-and-ship points)
1. Phase 1 + Phase 2 → foundation ready.
2. Phase 3 (US1) → pairing/connection proven on real TV.
3. Phase 4 (US2) → **usable everyday remote — first real ship**.
4. Phase 5 (US3) → navigation completes the "real remote" feel.
5. Phase 6 → installable PWA + away-from-home + validation pass.

### Notes
- Commit after each task or logical group; keep the tree runnable (Constitution: Dev Workflow).
- Verify story slices on the **real TV** before marking complete (Principle V).
- Total: **34 MVP tasks** (T001–T034) + 2 deferred.
