# Phase 0 Research: LG webOS TV Remote

All Technical Context unknowns resolved below. Format: Decision / Rationale / Alternatives.

## R1. How to control an LG webOS TV from code

**Decision**: Use the **SSAP** (Secure Socket Application Protocol) WebSocket interface that
webOS exposes on the LAN at `ws://<tv-ip>:3000` (or `wss://…:3001`), via the maintained
Node library **`lgtv2`** (hobbyquaker/lgtv2).

**Rationale**: SSAP is the de-facto local control protocol for webOS TVs and is what every
open-source LG remote uses. `lgtv2` handles the handshake, pairing, client-key persistence,
auto-reconnect, and the `request(uri, payload?, cb?)` call pattern. It exposes the
pointer-input socket needed for D-pad/OK/back/home buttons.

**Alternatives considered**:
- Raw `ws` + hand-rolled SSAP — more code, no benefit; `lgtv2` already encapsulates it.
- `aiowebostv` (Python) — solid, but Node keeps one language across back/front end.
- LG "IP control" (port 9761, newer models) — a different, more limited protocol; SSAP is
  richer (apps, inputs, pointer) and well supported.

## R2. The browser → TV mixed-content problem (central constraint)

**Decision**: Do **not** connect the browser to the TV directly. Put a **Node backend** in
the middle: browser ⟷ backend (HTTPS/WSS) ⟷ TV (`ws://`). The backend holds the only socket
to the TV.

**Rationale**: An installed PWA (served over HTTPS) is blocked by the browser from opening
insecure `ws://` to the TV, and the TV's `wss://` uses an untrusted self-signed cert that a
browser WebSocket can't be made to accept without ugly per-device steps. Server→TV has no
such restriction. This also lets the pairing key live in one trusted place and be shared by
all the user's devices (pair once, use everywhere) — directly serving Principle I.

**Alternatives considered**:
- Pure client-side PWA hitting the TV — blocked by mixed content / cert trust. Rejected.
- Native Android app (could open `ws://` directly, no backend) — but then iPad needs a
  separate app and we lose the single-codebase, install-anywhere goal. Rejected for this
  device mix.

## R3. Making it installable + reachable away from home, free, no cert fuss

**Decision**: Run the backend as plain HTTP on `localhost` on the Raspberry Pi, and expose it
with **Tailscale Serve** (`tailscale serve`), which terminates valid HTTPS at
`https://<machine>.<tailnet>.ts.net`.

**Rationale**: PWA installability (service worker + add-to-home-screen as a real app)
requires a trusted HTTPS origin. Tailscale issues a real Let's Encrypt cert for the tailnet
hostname automatically — no self-signed cert to install on each device. Because Tailscale is
a private overlay across the user's own devices, the same URL works at home and away,
satisfying SC-007, with control traffic never touching a third-party control service.
Tailscale is free for personal use and already cross-platform (Linux/Android/iPad).

**Alternatives considered**:
- Self-signed cert on the LAN + install root CA on phone/iPad — works but is exactly the
  "fuss" the user rejected. Rejected.
- Plain HTTP on the LAN + home-screen shortcut — Android won't install it as a real PWA
  over HTTP, and it wouldn't work away from home. Rejected as the primary path (kept as a
  documented fallback in `docs/setup-tailscale.md`).
- Cloudflare Tunnel / ngrok — exposes to the public internet and/or a third party; worse
  privacy posture than a private overlay. Rejected.

## R4. TV discovery

**Decision**: Use **SSDP** (`node-ssdp` client) to find LG TVs on the LAN, with **manual IP
entry** as a guaranteed fallback (FR-003). Persist the chosen TV so discovery is only needed
once.

**Rationale**: webOS TVs advertise over SSDP/UPnP; a short scan yields the IP without the
user reading it off the TV. Manual entry covers networks where multicast is flaky.

**Alternatives considered**: mDNS — less reliable for webOS than SSDP. Hard-coded IP only —
fails the "no-fuss discovery" goal.

## R9. How the official LG apps do this (and why we still need the Pi)

**Finding**: The official apps (LG TV Plus / ThinQ) use **two** mechanisms; there is no
separate server box — **the TV itself is the server/agent**.

- **Same Wi-Fi**: the app talks **directly** to the TV over the LAN using the *same* SSAP
  WebSocket protocol we use. It works without a backend only because it is a **native app**,
  which is not bound by the browser's mixed-content/self-signed-cert rules. A native app can
  open a raw socket to the TV and accept its cert; a browser cannot.
- **Away from home**: the TV keeps a persistent **outbound** connection to **LG's cloud**
  (no router port-forwarding needed). The app talks to LG cloud; the cloud relays the
  command back down to the TV. Requires an LG account + the TV signed in.

**Implication for our design**:
- We need the Pi **only because we chose a browser-based PWA** (one installable app across
  Android + iPad, no app store). The Pi plays, on the LAN, the role a native app plays for
  LG — it holds the socket the browser can't.
- Our Tailscale path is the private equivalent of LG's cloud relay for away-from-home use —
  with **no LG account and nothing through LG's cloud** (preserves Principle III).
- Even LG cannot reach a TV without an agent on its network; they embed the agent in the TV
  firmware. We can't modify the TV, so our agent lives on the Pi. This is also why a
  "remote server so others can use it" (multi-tenant) still requires a local agent per
  user's network — the cloud can only be a rendezvous/relay, never the controller.

**Alternatives reconsidered**: a **native app** (e.g. Android) would remove the backend for
home use — but would need a *separate* iPad app and still need a relay for away-from-home.
Rejected for this device mix; PWA + Pi keeps one codebase.

## R5. D-pad / OK / Back / Home buttons

**Decision**: Acquire the **pointer-input socket** via
`ssap://com.webos.service.networkinput/getPointerInputSocket`, then send button events
(`UP`, `DOWN`, `LEFT`, `RIGHT`, `ENTER`, `BACK`, `HOME`, `EXIT`). `lgtv2`'s specialized
socket exposes a `.button(name)` helper for this.

**Rationale**: Navigation keys are not plain SSAP request URIs; they go over the input
socket. `lgtv2` already wraps the socket acquisition and keepalive.

**Alternatives considered**: Faking navigation via app-specific SSAP calls — fragile and
incomplete. Rejected.

## R6. Connection-state model & resilience

**Decision**: A backend state machine with states **needs-pairing → connecting → connected
→ disconnected** plus **off-network**, derived from `lgtv2` events (`connecting`, `connect`,
`prompt`, `close`, `error`) and reachability checks. Push state to the UI over a backend
WebSocket; the UI always renders the current state (FR-009, FR-014).

**Rationale**: Principle IV demands the UI never lie about connectivity. Centralizing state
in the backend (the only party that truly knows the TV link) keeps every client consistent.

**Alternatives considered**: Letting the frontend infer state from request failures — racy
and inconsistent across devices. Rejected.

## R7. Where the pairing key + TV list live

**Decision**: A single JSON file `~/.config/lg-remote/store.json` on the Raspberry Pi,
`{ tvs: [{ id, name, address, clientKey }], activeId }`. The backend reads/writes it; the
key is passed to `lgtv2` via the `clientKey`/`saveKey` options so we own persistence.

**Rationale**: No accounts, minimal state (Principle III). Server-side storage means the
key is entered/approved once and shared by phone + iPad automatically.

**Alternatives considered**: Per-device browser storage of the key — would force re-pairing
per device and risk leaking the credential to the client. Rejected.

## R8. Testing without flaking on real hardware

**Decision**: A **mock webOS WebSocket server** (`backend/tests/mock-tv.ts`) that implements
the SSAP register/`client-key` handshake and acknowledges the command URIs, used by `vitest`
for deterministic unit/contract tests. The **real TV** is used for manual end-to-end
verification per `quickstart.md` (the user confirmed a real TV is available).

**Rationale**: Keeps CI/local tests fast and hardware-independent while still proving the
real integration via a scripted manual pass — satisfies Principle V's "demonstrated on real
hardware" bar.

**Alternatives considered**: Tests only against the real TV — slow, flaky, requires the TV
on. No tests — violates the workflow. Rejected.
