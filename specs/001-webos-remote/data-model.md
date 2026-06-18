# Phase 1 Data Model: LG webOS TV Remote

The system holds very little persistent state (Principle III). Entities below map directly
to the spec's Key Entities.

## TVConnection (persisted)

A remembered television. Stored in `~/.config/lg-remote/store.json`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | string | Stable id (TV's UPnP UUID if available, else a generated id). Primary key. |
| `name` | string | Friendly name (from SSDP/`getSystemInfo`, user-editable). |
| `address` | string | Last-known host/IP. May change (DHCP) — updated on rediscovery (FR-010). |
| `clientKey` | string \| null | Pairing credential from the TV. `null` until paired. **Sensitive.** |
| `lastConnectedAt` | ISO string \| null | For ordering / diagnostics. |

**Validation**: `id` and `name` non-empty; `address` a valid host or IPv4; `clientKey` is
opaque (never parsed, never sent to the frontend).

**Store shape**:

```jsonc
{
  "tvs": [ { "id": "...", "name": "Living Room TV", "address": "192.168.1.50", "clientKey": "…", "lastConnectedAt": "2026-06-18T20:00:00Z" } ],
  "activeId": "..."   // which TV is currently controlled; null if none
}
```

**Relationships**: 0..N `TVConnection`s; exactly one `activeId` (or null). One is active at a time (Assumptions).

## ControlCommand (transient, not persisted)

A single user action sent to the active TV.

| Field | Type | Notes |
|-------|------|-------|
| `type` | enum | `volumeUp` \| `volumeDown` \| `setMute` \| `playPause` \| `nav` \| (later: `launchApp`, `setInput`) |
| `params` | object? | e.g. `{ mute: true }` for `setMute`; `{ button: "UP" }` for `nav`. |
| `result` | enum | `acknowledged` \| `failed` (+ optional message). Returned to caller, not stored. |

**Validation (zod at the API boundary)**:
- `type` must be a known command.
- `nav.button` ∈ `{ UP, DOWN, LEFT, RIGHT, ENTER, BACK, HOME, EXIT }`.
- Commands are rejected (not queued indefinitely) unless state is `connected` or briefly
  `connecting` (FR-014 / edge case "command while reconnecting").

## ConnectionState (transient, derived, pushed to UI)

The backend's authoritative view of the link to the active TV.

| Field | Type | Notes |
|-------|------|-------|
| `status` | enum | `needs-pairing` \| `connecting` \| `connected` \| `disconnected` \| `off-network` |
| `tvId` | string \| null | The active TV this state refers to. |
| `volume` | number? | Last-known volume, when connected. |
| `muted` | boolean? | Last-known mute. |
| `message` | string? | Human-readable detail for failures (e.g. "TV not reachable"). |

**State transitions** (derived from `lgtv2` events + reachability):

```text
needs-pairing ──pair approved──▶ connecting ──connect──▶ connected
     ▲                                │                      │
     │ key rejected/cleared           │ error/close          │ close/error
     └────────────────────────────────┤                      ▼
                                       └────────────────▶ disconnected ──reachable again──▶ connecting
                                                              │
                                            no route to TV / phone off-net
                                                              ▼
                                                        off-network
```

- `prompt` event → `needs-pairing`.
- `connecting` event → `connecting`.
- `connect` event → `connected` (then query volume/mute to populate fields).
- `close`/`error` with route to host → `disconnected` (auto-reconnect loop runs).
- No route / multicast+ping fail → `off-network`.
