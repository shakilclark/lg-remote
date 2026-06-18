# Quickstart & Validation: LG webOS TV Remote

How to run the app and verify each user story end-to-end. Implementation details live in
`tasks.md`; this is the run/verify guide.

## Prerequisites

- Node.js 20 LTS on the Raspberry Pi (and for dev, on the MacBook).
- An LG webOS TV powered on and on the same LAN.
- (For install + away-from-home) Tailscale installed on the Raspberry Pi, the Android phone,
  and the iPad, all logged into the same tailnet. See `docs/setup-tailscale.md`.

## Install & run (dev)

```bash
# from repo root
cd backend && npm install
cd ../frontend && npm install

# Terminal 1 — backend (serves API + built UI)
cd backend && npm run dev        # http://localhost:8080

# Terminal 2 — frontend (Vite dev server, proxies /api to backend)
cd frontend && npm run dev       # http://localhost:5173
```

## Run on the Raspberry Pi (production)

```bash
cd frontend && npm run build     # outputs to ../backend/public
cd ../backend && npm start       # serves UI + API on :8080
tailscale serve --bg 8080        # expose as https://<pi>.<tailnet>.ts.net
```

Then on the phone/iPad: open `https://<pi>.<tailnet>.ts.net` in the browser and choose
**Add to Home Screen / Install app**.

## Validation scenarios

### US1 — Pair once (P1)
1. Fresh device, open the app → it discovers the TV (or prompts for IP).
2. Tap connect → **TV shows an authorization prompt**; accept it on the TV.
3. App shows **connected**. ✅ Expected: SC-001 (< 60s, excluding the on-TV prompt wait).
4. Close and reopen the app → reconnects **with no prompt** in < 5s. ✅ SC-002.
> Contract: `POST /api/discover`, `POST /api/tvs`, `POST /api/pair` (see contracts/backend-api.md).

### US2 — Volume & playback (P1)
1. Tap volume-up/down → TV volume changes; level reflected in the app within 1s. ✅ SC-003.
2. Tap mute → TV mutes/unmutes; app shows mute state.
3. Play something, tap play/pause → playback toggles.
> Contract: `POST /api/command` with `volumeUp`/`volumeDown`/`setMute`/`playPause`.

### US3 — Navigation (P2)
1. Use the D-pad → on-screen focus moves in each direction.
2. Tap OK → highlighted item activates.
3. Tap back / home → returns / goes to webOS home.
> Contract: `POST /api/command` `{ "type": "nav", "params": { "button": "…" } }`.

### Resilience checks (FR-009/010/014, SC-005)
- Turn the TV off → app shows **disconnected**, commands return a visible failure (not silent).
- Turn the TV back on → app **auto-reconnects** without re-pairing.
- Put the phone on mobile data **without** Tailscale → app shows **off-network**.
- With Tailscale on, phone on mobile data → app still controls the TV. ✅ SC-007.

## Automated tests

```bash
cd backend && npm test     # vitest against the mock webOS server (no TV needed)
cd frontend && npm test     # vitest + Testing Library (component/state)
```

## Install check (SC-004)
On Android Chrome and iPad Safari, the app installs to the home screen and launches
full-screen with **no browser address bar**.

## Validation log

### 2026-06-18 — real TV (192.168.0.9), Android phone on home Wi-Fi
- ✅ **US1** Pair once: paired from the phone (TV prompt accepted); reopening reconnects
  with no prompt. Backend auto-reconnected on restart (`status: connected`). SC-001/002.
- ✅ **US2** Volume/mute/play-pause: confirmed on the phone; volume reflected live
  (1 → 3 via real `volumeStatus`). SC-003.
- ✅ **US3** Navigation: HOME + arrows moved on-screen focus on the TV (pointer-input socket).
- ✅ Backend tests: 15 passing against the mock webOS server.
- ✅ PWA assets served (manifest `display: fullscreen`, icons, service worker).
- ✅ **Tailscale HTTPS live**: `tailscale serve --bg 8080` →
  `https://mac.tail101494.ts.net/` returns 200 (valid cert). Secure origin now available,
  unblocking Android install (SC-004) and the US5 motion cursor.
- ⏳ **SC-004 install / SC-007 away-from-home**: ready to verify on the phone via the
  HTTPS URL (install + away-from-home).
- ⏳ iPad pass: not yet run.
