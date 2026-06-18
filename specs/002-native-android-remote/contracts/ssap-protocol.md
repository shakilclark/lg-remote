# Contract: webOS SSAP (Android app ⟷ TV, direct)

What the Kotlin SSAP client depends on from the TV. This replaces `001`'s
`contracts/tv-protocol.md` (which went via `lgtv2`); the wire protocol is the same, but now the
app speaks it directly. Source of truth: the verified `001` backend + Connect-SDK reference.

## Connection & TLS

- Connect to **`wss://<tv-ip>:3001`** (secure SSAP). Do **not** use cleartext `ws://:3000`.
- The TV presents a **self-signed certificate**. The OkHttp client MUST install a custom
  `X509TrustManager` that accepts it and a `HostnameVerifier` that accepts the TV's host/IP.
  (LAN-scoped trust-this-host; never shown to the user — Principle I, FR-016.)
- Reuse the **same** configured `OkHttpClient` for the secondary pointer socket (also `wss://`,
  also self-signed).

## Handshake / pairing

1. On open, send a `register` message:
   ```json
   { "id": "register_0", "type": "register",
     "payload": { "pairingType": "PROMPT",
                  "client-key": "<stored key or omitted>",
                  "manifest": { /* permissions manifest, as Connect-SDK/lgtv2 */ } } }
   ```
2. TV responds:
   - `{ "type":"registered", "payload": { "client-key": "<key>" } }` → **persist the key**,
     state → `Connected`.
   - A `PROMPT` response first (user must accept on the TV) → state → `NeedsPairing`; on accept
     the `registered`/`client-key` arrives → `Connected`.
3. On later launches, send the stored `client-key` → silent reconnect (no prompt) — FR-002.

## Request/response framing

- Requests: `{ "id": "<unique>", "type": "request", "uri": "ssap://…", "payload": { … } }`.
- Correlate responses by `id`. Errors arrive as `{ "type":"error", "error":"…", "id":… }`.
- Subscriptions: `type: "subscribe"` keeps a stream (used for volume/mute).

### Command URIs

| App command | SSAP request | Payload |
|-------------|--------------|---------|
| Volume up | `ssap://audio/volumeUp` | — |
| Volume down | `ssap://audio/volumeDown` | — |
| Set mute | `ssap://audio/setMute` | `{ "mute": true \| false }` |
| Get/subscribe volume | `ssap://audio/getVolume` | — (subscribe; keeps UI in sync) |
| Play | `ssap://media.controls/play` | — |
| Pause | `ssap://media.controls/pause` | — |
| System info (name) | `ssap://system/getSystemInfo` | — |
| List apps | `ssap://com.webos.applicationManager/listLaunchPoints` | — |
| Launch app | `ssap://system.launcher/launch` | `{ "id": "<appId>" }` |
| List inputs | `ssap://tv/getExternalInputList` | — |
| Switch input | `ssap://tv/switchInput` | `{ "inputId": "<id>" }` |

Notes:
- **playPause**: single UI toggle; track last media state to choose `play` vs `pause`, default
  to `pause` when unknown (per `001`).
- **getVolume** returns either flat `{ volume, muted }` or nested `{ volumeStatus: { volume,
  muteStatus } }` depending on firmware — handle both (observed on the real TV in `001`).
- **launchApp** app ids vary by webOS version → match `listLaunchPoints` titles ("YouTube",
  "Netflix") to the real id; fall back to `youtube.leanback.v4` / `netflix`; detect
  not-installed from the launch result (US6 #3).

## Pointer-input socket (nav buttons + motion cursor)

D-pad/OK/back/home and the cursor are **not** request URIs:
1. Request `ssap://com.webos.service.networkinput/getPointerInputSocket`.
2. Response payload carries `socketPath` — a `wss://` URL. Open a **second** WebSocket to it
   (same TrustManager).
3. Send newline-delimited frames (blank line terminates):
   ```
   type:button
   name:UP            # UP|DOWN|LEFT|RIGHT|ENTER|BACK|HOME|EXIT

   type:move
   dx:12
   dy:-8
   down:0

   type:click

   ```
- Buttons (US3) and `move`/`click` (US5 cursor) share this one socket. Keep it open while
  Connected; reopen if it drops.

## Discovery (later slice)

- SSDP M-SEARCH for `urn:lge-com:service:webos-second-screen:1` (or scan + filter LG), requires
  a `WifiManager.MulticastLock`. Confirm name via `getSystemInfo` after connect.
- v1 fallback / primary: **manual IP entry**.

## Contract tests (MockWebServer)

- `register` with stored key → `registered` (no prompt path) and without key → prompt →
  `registered`.
- Each command URI → correct outgoing JSON; response routed to the right pending `id`.
- `getVolume` flat **and** nested shapes both parse to `(volume, muted)`.
- `getPointerInputSocket` → opens the secondary socket → button/move/click frames match the
  byte format above.
- Error response → `Failed(reason)`, surfaced not swallowed.
