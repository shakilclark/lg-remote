# Contract: webOS SSAP (backend ⟷ TV)

What the backend depends on from the TV, via `lgtv2`. This is the external integration
contract — if `lgtv2`/webOS changes, this is the file to revisit.

## Connection & pairing

- Connect to `ws://<tv-ip>:3000` (insecure; backend-side only, allowed).
- On connect, `lgtv2` sends a registration payload. The TV either:
  - returns a `client-key` (already-approved device) → `connect` event, or
  - emits a `prompt` event → user must approve on the TV → then `client-key` arrives.
- We pass/receive the key via `clientKey` (in) and `saveKey` (out) options so it is stored
  in our `store.json`, not only in `lgtv2`'s default keyfile.
- `reconnect: 5000` → auto-reconnect every 5s while disconnected.

### Events consumed
`connecting`, `connect`, `prompt`, `close`, `error` → mapped to `ConnectionState`
(see data-model.md).

## Command URIs (request URIs)

| App command | SSAP request | Payload |
|-------------|--------------|---------|
| Volume up | `ssap://audio/volumeUp` | — |
| Volume down | `ssap://audio/volumeDown` | — |
| Set mute | `ssap://audio/setMute` | `{ "mute": true \| false }` |
| Get volume/mute | `ssap://audio/getVolume` | — (subscribe to keep UI in sync) |
| Play | `ssap://media.controls/play` | — |
| Pause | `ssap://media.controls/pause` | — |
| System info (name) | `ssap://system/getSystemInfo` | — |
| (later) List apps | `ssap://com.webos.applicationManager/listLaunchPoints` | — |
| (later) Launch app | `ssap://system.launcher/launch` | `{ "id": "<appId>" }` |
| (later) List inputs | `ssap://tv/getExternalInputList` | — |
| (later) Switch input | `ssap://tv/switchInput` | `{ "inputId": "<id>" }` |

> Play/pause: the app exposes a single `playPause` toggle; the backend tracks the last
> media state to choose `play` vs `pause`, falling back to `pause` when unknown.

## Pointer-input socket (navigation buttons)

D-pad/OK/back/home are **not** request URIs. The backend:
1. Requests `ssap://com.webos.service.networkinput/getPointerInputSocket`.
2. Receives a `socketPath` (a secondary WebSocket) — `lgtv2` wraps this.
3. Sends button events via the wrapped socket's `.button(name)`:
   `UP`, `DOWN`, `LEFT`, `RIGHT`, `ENTER` (OK), `BACK`, `HOME`, `EXIT`.

## Discovery (SSDP)

- M-SEARCH for LG webOS devices (`urn:lge-com:service:webos-second-screen:1`, or scan and
  filter by LG signature). Yields candidate IP(s); name confirmed via `getSystemInfo` after
  connect. Manual IP entry is the fallback.
