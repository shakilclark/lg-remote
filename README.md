# LG webOS TV Remote

An installable, touch-first remote for an LG webOS TV. Built spec-first with
[GitHub Spec Kit](https://github.github.com/spec-kit/) — see [`specs/001-webos-remote/`](specs/001-webos-remote/).

## How it works

A browser can't open the TV's insecure local WebSocket from a secure/installed app, so a
small **Node backend** (run on an always-on box — a Raspberry Pi) owns the only socket to
the TV, persists the pairing key, and serves the **PWA** UI. **Tailscale Serve** fronts it
with real HTTPS so the PWA installs cleanly on Android + iPad and works away from home — all
private, no third-party cloud.

```
phone/iPad (PWA) ──HTTPS/WSS (Tailscale)──▶ Node backend (Pi) ──ws:// SSAP──▶ LG TV
```

## Status

| Slice | Scope | State |
|-------|-------|-------|
| US1 | Pair once, auto-reconnect, live connection state | ✅ built |
| US2 | Volume / mute / play-pause | ⏳ next |
| US3 | D-pad / OK / Back / Home | ⏳ next |
| Polish | PWA install, Tailscale docs, off-network detection | ⏳ next |

## Run it (dev)

```bash
cd backend && npm install
cd ../frontend && npm install && npm run build   # emits into backend/public

cd ../backend && PORT=8080 npm run dev            # serves UI + API on :8080
```

Open `http://<this-machine-ip>:8080` on a phone on the same Wi-Fi. Scan for the TV (or
enter its IP), then accept the pairing prompt on the TV.

- Backend: `backend/` (Node + TypeScript, `lgtv2`, `node-ssdp`, Express, `ws`).
- Frontend: `frontend/` (React + TypeScript + Vite).
- Tests: `cd backend && npm test` (real `lgtv2` against a mock webOS server — no TV needed).

State lives in `~/.config/lg-remote/store.json` (override with `LG_REMOTE_STORE`).
