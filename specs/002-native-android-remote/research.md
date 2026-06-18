# Phase 0 Research — Native Android Direct-LAN Remote

Decisions backing the plan. Verified via the deep-research workflow (3-0 adversarial votes
unless noted) and the working `001` backend (protocol source of truth).

## R1 — TV transport: `wss://<tv-ip>:3001` + custom TrustManager (not cleartext `:3000`)

**Decision**: Connect over the **secure** SSAP socket `wss://<tv-ip>:3001` and trust the TV's
self-signed certificate programmatically with a custom `X509TrustManager` + `HostnameVerifier`
on the OkHttp client.

**Rationale**: LG deprecated the cleartext `:3000` socket (Jan 2023 policy); mature references
(Connect-SDK `WebOSTVServiceSocketClient`, `heroslender/lg-remote`) use `wss://:3001`. A native
app can trust the self-signed cert in code — exactly the capability a browser lacks, which is
why `001` needed a server. Relying on cleartext would also drag in Android's cleartext opt-in
and the deprecation risk.

**Alternatives rejected**: cleartext `ws://:3000` (deprecated, may break on new firmware);
shipping a pinned cert (the TV's cert is per-device/self-signed — pin-to-trust-all for this
host is the pragmatic LAN choice).

## R2 — SSAP client written from scratch in Kotlin (OkHttp)

**Decision**: Hand-write the SSAP client on `OkHttpClient.newWebSocket`. Model requests as
`{ id, type:"request", uri, payload }` and correlate responses by `id`; support `subscribe`
for volume/mute. `@Serializable` data classes via kotlinx.serialization.

**Rationale**: The protocol is small and fully proven in the `001` backend. From-scratch avoids
an archived dependency (Connect-SDK, ~2015, Java) and gives clean coroutine/Flow ergonomics and
testability (MockWebServer). The `001` `lgtv2` behaviour and `contracts/ssap-protocol.md` pin
the exact message shapes.

**Alternatives rejected**: fork Connect-SDK (archived, Java, heavy); fork `heroslender/lg-remote`
(inherits its UI/arch — we're redesigning UI in Compose). Both kept as read-only references.

## R3 — Pairing & client-key (carried over from `001`)

**Decision**: On connect, send the `register` handshake with `pairingType: "PROMPT"` and the
stored `client-key` if present. If no key, the TV emits a prompt → user accepts on the TV → the
TV returns `client-key`, which we persist in DataStore. Reuse the key on every reconnect.

**Rationale**: Identical to the verified `001` flow and to Connect-SDK's `register` payload.
Idempotent when already paired.

## R4 — Pointer-input socket (nav buttons + cursor)

**Decision**: Request `ssap://com.webos.service.networkinput/getPointerInputSocket`; the
response carries a `socketPath` (a `wss://` URL) → open a **second** OkHttp WebSocket to it.
Send newline-delimited frames: `type:button\nname:UP\n\n`, `type:move\ndx:..\ndy:..\ndown:0\n\n`,
`type:click\n\n`. Buttons: UP/DOWN/LEFT/RIGHT/ENTER/BACK/HOME/EXIT.

**Rationale**: Connect-SDK `WebOSTVMouseSocketConnection` emits exactly this wire format and it
matches `001`. The cursor (US5) reuses this socket's `move`/`click` — no new TV capability.

## R5 — Motion cursor via `SensorManager` (no HTTPS, no web permission)

**Decision**: Use `TYPE_GAME_ROTATION_VECTOR` (fallback `TYPE_GYROSCOPE`) registered only while
the cursor button is held. Convert angular delta → `dx/dy`, apply a dead-zone + low-pass
smoothing, stream as `move` frames on the pointer socket. Unregister on release (hold-gate).

**Rationale**: Native sensors need no HTTPS and no runtime permission (motion sensors aren't a
dangerous permission), removing the `001` DeviceOrientation/HTTPS dependency entirely — a core
reason native suits this feature. Tunables (sensitivity, dead-zone) carry over from the `001`
`CursorPad` first-pass (SENSITIVITY≈14, DEAD_ZONE≈0.25) as starting points to retune on-device.

## R6 — Discovery: manual IP first, SSDP later

**Decision**: v1 ships **manual IP entry** (user's TV at a known address). SSDP auto-discovery
(M-SEARCH `urn:lge-com:service:webos-second-screen:1`, requires a `WifiManager.MulticastLock`)
is a later polish slice; multi-TV picker (FR-015) follows discovery.

**Rationale**: Manual entry is reliable and unblocks all control slices immediately; SSDP adds
multicast-lock complexity that isn't on the MVP critical path.

## R7 — Persistence: AndroidX DataStore (Preferences)

**Decision**: Store TV records (name, last address, client-key) + active-TV id in Preferences
DataStore under `filesDir`. No DB needed at this scale.

**Rationale**: App-private, async/Flow-native, no schema overhead for a handful of records.
Satisfies Principle III (key stays on-device).

## R8 — Android platform: minSdk 31, permissions, full-screen, volume keys

**Decisions**:
- `minSdk 31` / `targetSdk` current. Modern-only simplifies TLS + permission handling.
- Manifest: `INTERNET`, `ACCESS_NETWORK_STATE`; add `CHANGE_WIFI_MULTICAST_STATE` when SSDP
  lands. For `targetSdk 37+` (Android 17) add the runtime **`ACCESS_LOCAL_NETWORK`** permission
  with a rationale UI and graceful-deny path (FR-017).
- Full-screen edge-to-edge (`enableEdgeToEdge`); portrait-locked.
- Capture the hardware volume rocker in `MainActivity.onKeyDown` (VOLUME_UP/DOWN) → TV volume,
  consuming the event while foregrounded (US2 #4).

**Rationale**: Matches FR-011/FR-012/FR-017 and the "good native app" bar.

## R9 — Reference implementations (read-only)

- `heroslender/lg-remote` — 100% Kotlin, maintained (v0.5.2, May 2026): architecture/UX cues.
- LG `Connect-SDK-Android-Core` (archived ~2015): exact SSAP URIs, `register` handshake, and
  `WebOSTVMouseSocketConnection` pointer wire format.
- `001` `backend/src/tv/` (this repo): the behaviour we already verified against the real TV
  (nested `volumeStatus` shape, app-id resolution fallbacks, off-network error mapping).

## Open risk notes (not blocking)

- Some firmware split on whether `:3000` still answers (research vote 1-2). We sidestep by
  using `:3001` only.
- `getPointerInputSocket` returns a `wss://` path that may itself be self-signed → reuse the
  same `TrustManager`/client.
- `ACCESS_LOCAL_NETWORK` only matters once we bump to `targetSdk 37+`; track when toolchain
  defaults move.
