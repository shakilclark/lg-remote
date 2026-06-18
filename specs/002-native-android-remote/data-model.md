# Phase 1 Data Model — Native Android Remote

Small, on-device only. No server, no DB — Preferences DataStore holds a handful of records.

## Entities

### TVConnection
A remembered television.

| Field | Type | Notes |
|-------|------|-------|
| `id` | String | Stable id (derived from the TV's UDN/device id; falls back to address). |
| `name` | String | Friendly name (from `ssap://system/getSystemInfo`, else user-entered/IP). |
| `address` | String | Last-known IP on the LAN. May change (DHCP) → updated on reconnect. |
| `clientKey` | String? | Pairing credential from the TV. **Never leaves the device.** Null until paired. |

- Persisted in DataStore. A small set may be remembered; exactly one is **active**.

### ActiveSelection
| Field | Type | Notes |
|-------|------|-------|
| `activeId` | String? | Which `TVConnection` is currently targeted. Null = none configured. |

### ControlCommand (transient, not persisted)
A single user action sent to the active TV.

```
sealed interface ControlCommand
  VolumeUp / VolumeDown
  SetMute(mute: Boolean)
  PlayPause
  Nav(button: NavButton)            // UP|DOWN|LEFT|RIGHT|ENTER|BACK|HOME|EXIT
  LaunchApp(app: AppKey)            // YouTube | Netflix (US6)
  SetInput(inputId: String)         // US7
  CursorMove(dx: Float, dy: Float)  // US5 (pointer socket)
  CursorClick                       // US5
```
Result: `Acknowledged` | `Failed(reason)`.

### ConnectionState (surfaced in UI; FR-009)
```
sealed interface ConnectionState
  NeedsPairing(message)             // TV showing the accept prompt
  Connecting
  Connected(volume: Int?, muted: Boolean?)
  Disconnected(message)             // TV off/asleep/unreachable
  OffNetwork(message)               // phone not on the TV's LAN
  PermissionRequired                // Android local-network permission denied (FR-017)
```

### TVInput (transient; US7)
| Field | Type | Notes |
|-------|------|-------|
| `id` | String | e.g. `HDMI_2`. |
| `label` | String | Device label, e.g. "PS4 Game Console"; fallback to id. |

## State machine (ConnectionState transitions)

```
                 ┌────────────► OffNetwork ◄────────────┐  (phone leaves Wi-Fi)
                 │                                       │
   app start     │                                       │
      │          │   no LAN / permission denied          │
      ▼          │                                       │
  (has active?) ─┴─ no ─► [Connect screen: manual IP / pick] ─► add TV
      │ yes
      ▼
  Connecting ──► register(clientKey?) ──► has key & approved ──► Connected
      │                     │
      │                     └─ no key / prompt ──► NeedsPairing ─(accept on TV)─► Connected
      │
      ├─ socket error / TV asleep ──► Disconnected ──(auto-retry, IP rediscover)──► Connecting
      └─ local-network perm denied ──► PermissionRequired ──(granted)──► Connecting
```

- **Auto-reconnect**: while Disconnected and on-network, retry on a backoff; on each attempt
  refresh the address (handles DHCP change) — FR-010.
- **Command while not Connected**: rejected with a visible reason (FR-014); never silently
  dropped. Cursor frames are only sent while Connected and the hold-gate is active.
- `Connected` carries the latest volume/mute from the `getVolume` subscription so the UI stays
  in sync without polling (handle both flat and nested `volumeStatus` shapes per `001`).

## Persistence rules

- Only `TVConnection` (incl. `clientKey`) and `activeId` are persisted, in app-private DataStore.
- `clientKey` is treated as a credential: never logged, never sent anywhere except the TV's
  `register` handshake.
- No analytics, no network calls other than to the TV (Principle III / FR-013).
