# Contract: Backend API (frontend ⟷ backend)

Base URL: the Tailscale HTTPS origin (e.g. `https://thinkpad.<tailnet>.ts.net`). All paths
under `/api`. JSON bodies. No auth in v1 (private overlay; single user) — noted as a future
hardening item.

## REST

### `GET /api/tvs`
List remembered TVs and which is active.
```json
200 → { "tvs": [ { "id": "...", "name": "Living Room TV", "address": "192.168.1.50", "paired": true } ], "activeId": "..." }
```
> `clientKey` is **never** included in any response.

### `POST /api/discover`
Trigger an SSDP scan (a few seconds). Returns TVs found on the LAN.
```json
200 → { "found": [ { "id": "...", "name": "...", "address": "192.168.1.50" } ] }
```

### `POST /api/tvs`
Add a TV by discovered id or manual address, and make it active.
```json
body → { "address": "192.168.1.50", "name": "Living Room TV" }   // name optional
200  → { "id": "...", "activeId": "..." }
```

### `POST /api/pair`
Begin/continue pairing the active TV. The TV shows an on-device prompt; resolves once
approved. Idempotent if already paired.
```json
200 → { "status": "needs-pairing" | "connected", "message": "Accept the prompt on your TV" }
```

### `POST /api/command`
Send a control action to the active TV.
```json
body → { "type": "volumeUp" }
body → { "type": "setMute", "params": { "mute": true } }
body → { "type": "playPause" }
body → { "type": "nav", "params": { "button": "UP" } }   // UP|DOWN|LEFT|RIGHT|ENTER|BACK|HOME|EXIT
body → { "type": "launchApp", "params": { "app": "youtube" } }   // youtube|netflix (US6)
body → { "type": "setInput", "params": { "inputId": "HDMI_2" } } // (US7)

200 → { "result": "acknowledged", "state": { "status": "connected", "volume": 13, "muted": false } }
409 → { "result": "failed", "message": "TV not connected", "state": { "status": "disconnected" } }
422 → { "result": "failed", "message": "Unknown command" }
```

### `GET /api/inputs`  (US7)
List the TV's external inputs/sources.
```json
200 → { "inputs": [ { "id": "HDMI_1", "label": "HDMI 1" }, { "id": "HDMI_2", "label": "PS4 Game Console" } ] }
```
Latency target: < 1s round-trip on LAN (SC-003).

### `GET /api/state`
Current connection state snapshot (the same shape pushed over WS).
```json
200 → { "status": "connected", "tvId": "...", "volume": 13, "muted": false }
```

## WebSocket: `GET /api/cursor` (upgrade)  (US5 motion cursor)

Client→server, low-latency. The phone streams pointer deltas while the cursor button is held,
forwarded to the TV's pointer-input socket. Read by the backend only.
```json
→ { "type": "move", "dx": 12, "dy": -8 }
→ { "type": "click" }
```

## WebSocket: `GET /api/events` (upgrade)

Server→client push of live `ConnectionState` whenever it changes (FR-009). The client never
sends control over this socket (control goes via `POST /api/command`); it is read-only state.

```json
← { "type": "state", "state": { "status": "connecting", "tvId": "..." } }
← { "type": "state", "state": { "status": "connected", "tvId": "...", "volume": 13, "muted": false } }
← { "type": "state", "state": { "status": "needs-pairing", "tvId": "...", "message": "Accept the prompt on your TV" } }
← { "type": "state", "state": { "status": "off-network", "message": "Not on the same network as your TV" } }
```

## Error conventions
- `409` connection-state conflict (e.g. command while disconnected).
- `422` invalid command/params (zod validation).
- `503` no active TV configured.
- Bodies always include a human-readable `message` for the UI to surface (FR-014).
