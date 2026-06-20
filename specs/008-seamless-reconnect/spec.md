# Feature Specification: Seamless Reconnect (lock / background / resume)

**Feature Branch**: `008-seamless-reconnect`

**Created**: 2026-06-20

**Status**: Draft

**Builds on**: `002-native-android-remote` (the connection/`TvConnectionManager`), and relates to
`003-lockscreen-controls` (a foreground service is the heavier path to *staying* connected while
locked). This spec makes returning to the app feel instant and never show a jarring "disconnected"
state for a normal lock/unlock.

**Input**: User description: "the connection drops on lock, the re-join experience is terrible —
spec something elegant and as seamless as possible."

## Why (the observed problem)

When the phone locks (or the app is backgrounded), the SSAP WebSocket to the TV drops — the process
is suspended/dozed and the socket closes. Today the re-join is poor for three concrete reasons in the
current code:

1. **Flat 5-second reconnect delay.** `TvConnectionManager.reconnectDelayMs = 5_000` — every drop
   waits a full 5s before even *trying* to reconnect. Returning to the app means staring at a broken
   remote for seconds.
2. **Immediate "disconnected" screen on any blip.** `App.kt` routes to the `ReconnectView`
   ("Can't reach your TV") the instant a connected session becomes `Connecting`/`Disconnected`
   (`showReconnect = !connected && hasActiveTv && transient`). A 300ms reconnect still flashes the
   error screen.
3. **Live state is cleared on the drop.** On any non-`Connected` state the ViewModel clears inputs,
   stops volume/now-playing, closes the pointer — so even a momentary drop wipes the remote's context
   and it has to rebuild from scratch.

The result: lock → unlock → "Can't reach your TV" → wait → it rebuilds. The fix is to treat a
lock/unlock as a *non-event* visually, reconnect instantly on return, and only surface a real
problem when the TV is genuinely unreachable.

## Goal / target experience

Lock the phone mid-use; unlock a minute later → **the remote is already there and usable**, with no
"disconnected" flash, no spinner, no re-pair, no rebuild. If you tap a control the instant you
return, it works (or is held for the sub-second reconnect). The full reconnect/connect screen
appears **only** when the TV is actually off/asleep/off-network for more than a short grace period.

## Approach (two tiers)

- **Tier 1 — Instant-resume + optimistic UI (no service). RECOMMENDED for v1.** Reconnect eagerly the
  moment the app returns to the foreground; keep showing the last-known remote during a short grace
  window instead of the error screen; don't tear down live state on a transient drop; replace the
  flat 5s retry with fast backoff. Gets ~90% of the seamlessness with zero battery/notification cost.
- **Tier 2 — Foreground-service persistence (keep the socket alive while locked).** A foreground
  service holds the SSAP connection open even when backgrounded/locked, so there's *nothing* to
  reconnect on return. This is the same mechanism `003` (lockscreen controls) needs, so it should be
  built **with** `003`, not separately. Trade-off: a persistent notification + battery; OEM battery
  managers may still kill it. Out of scope for this spec's v1; called out so the two features align.

This spec specifies **Tier 1** as the deliverable and defines how it composes with Tier 2 later.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Instant, invisible re-join after lock (Priority: P1)

I'm using the remote, the phone locks (or I switch apps) and the connection drops in the background.
When I come back, the remote is immediately usable — I don't see a "disconnected" screen or a
spinner, and I don't have to re-pair or wait.

**Why this priority**: This is the entire complaint. A remote that punishes you for locking your
phone is worse than the physical remote; instant resume is the whole point.

**Independent Test**: With a connected TV, lock the phone, wait ~30s, unlock and return to the app →
the remote is interactive within a moment, with no error screen shown, and the first control press is
honoured.

**Acceptance Scenarios**:

1. **Given** a connected remote, **When** the phone locks and the socket drops in the background,
   **Then** on returning to the foreground the app reconnects **immediately** (no fixed multi-second
   delay).
2. **Given** the app is reconnecting on resume, **When** the reconnect completes within the grace
   window, **Then** the user never sees the "Can't reach your TV" screen — the remote stays on screen
   throughout.
3. **Given** I tap a control during the brief reconnect, **When** the socket comes back within ~1–2s,
   **Then** that action is sent on reconnect (not silently dropped or shown as an error). *(Optional —
   see OQ.)*
4. **Given** a previously paired TV, **When** the app reconnects, **Then** it reuses the stored
   client-key and never shows a pairing prompt.

---

### User Story 2 - No false "disconnected" flash (Priority: P1)

A momentary drop (lock/unlock, brief Wi-Fi blip, app switch) must not flash the error/reconnect
screen or wipe the remote. The remote stays visible, with at most a subtle, non-blocking
"reconnecting…" hint, and only escalates to the full reconnect screen if the TV stays unreachable.

**Why this priority**: The flashing error screen is the visible symptom of "terrible re-join"; even
with fast reconnect, a 300ms flash of red feels broken.

**Independent Test**: Cause a brief drop (toggle Wi-Fi off/on quickly, or lock/unlock) → observe the
remote stays rendered with a subtle reconnecting affordance, no full-screen error, and it settles
back to normal automatically.

**Acceptance Scenarios**:

1. **Given** a connected remote drops transiently, **When** less than the grace period (e.g. ~3s) has
   elapsed, **Then** the remote UI remains shown (controls visible) with an unobtrusive
   "reconnecting…" indicator — not the full reconnect screen.
2. **Given** the drop lasts beyond the grace period, **When** the TV is still unreachable, **Then**
   the app shows the proper reconnect screen (or off-network state) — honest about a real problem.
3. **Given** live state (volume, now-playing) was known before the drop, **When** within the grace
   window, **Then** it's shown as last-known (not blanked); it refreshes once reconnected and clears
   only if the drop becomes a real disconnect.

---

### User Story 3 - Don't thrash or drain while away (Priority: P2)

While the app is backgrounded/locked, it shouldn't pointlessly retry-loop a dropped connection
forever (battery, churn). It should back off while away and reconnect decisively on return.

**Why this priority**: The current 5s loop keeps firing in the background to no benefit; resume is
when reconnect actually matters.

**Independent Test**: Background the app with the TV off; confirm reconnect attempts back off (don't
hammer every 5s indefinitely) and that returning to the foreground triggers an immediate attempt.

**Acceptance Scenarios**:

1. **Given** the app is backgrounded and the socket dropped, **When** it stays backgrounded, **Then**
   reconnect attempts use increasing backoff (or pause) rather than a fixed tight loop.
2. **Given** the app returns to the foreground, **When** it resumes, **Then** a reconnect is
   attempted immediately regardless of where the backoff timer was.

---

### Edge Cases

- **Genuinely off-network** (phone left Wi-Fi): distinguish from a transient drop via `NetworkMonitor`
  — show the off-network state rather than an endless "reconnecting…".
- **TV actually off/asleep**: after the grace period, escalate to the reconnect screen; don't pretend
  to be connected forever.
- **Rapid lock/unlock**: must not stack multiple reconnects or flicker; reconnect is idempotent.
- **client-key invalidated** (TV factory-reset/forgot the device): reconnect surfaces the pairing
  prompt (`NeedsPairing`), not a silent failure loop.
- **Doze / process death**: on cold return, restore to the connected remote as fast as a warm resume
  allows, reusing the stored TV + client-key; no manual reconnect tap required.
- **Grace window during a real outage**: the "reconnecting…" hint must have a definite end — it
  escalates, it doesn't spin indefinitely.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On returning to the foreground (lifecycle resume), the app MUST attempt to reconnect
  **immediately** if not connected, independent of any pending backoff timer.
- **FR-002**: The app MUST replace the fixed 5s reconnect delay with **fast backoff** for unexpected
  drops while foregrounded (short initial delay, increasing, capped), so a recoverable blip recovers
  in well under a second where the network allows.
- **FR-003**: A transient drop of a previously-connected session MUST NOT immediately route to the
  full reconnect/connect screen. The app MUST keep rendering the remote during a **grace window** and
  show only a subtle, non-blocking "reconnecting…" affordance.
- **FR-004**: If the connection is not restored within the grace window, the app MUST escalate to the
  appropriate full state (reconnect screen, or off-network) — never spin "reconnecting…" forever.
- **FR-005**: Live state (volume, now-playing, inputs) MUST NOT be torn down on a transient drop
  within the grace window; it MAY be shown as last-known/stale and MUST refresh on reconnect, clearing
  only when the drop becomes a real disconnect.
- **FR-006**: Reconnects MUST reuse the persisted client-key and complete silently (no pairing prompt)
  unless the TV has actually revoked pairing.
- **FR-007**: While backgrounded with a dropped socket, the app MUST NOT retry in a tight fixed loop;
  it MUST back off (or suspend retries) and rely on FR-001 for immediate resume on return.
- **FR-008**: Reconnect logic MUST be idempotent under rapid lock/unlock — no stacked sockets, no
  visual flicker.
- **FR-009**: The app MUST continue to distinguish "transient drop" from "off-network" (via
  `NetworkMonitor`) and from "TV asleep/unreachable", surfacing the honest state after the grace
  window.
- **FR-010 (optional)**: A control pressed during the grace window MAY be **held briefly** and sent on
  reconnect (debounced, with a short max age) instead of being rejected; stale/aged actions MUST be
  dropped, not sent late. *(See OQ — risk of sending an unwanted action; default off if unsure.)*

### Key Entities *(include if feature involves data)*

- **Reconnect policy**: initial delay, backoff factor, cap, and the foreground-resume override.
  Not persisted; lives in the connection manager.
- **Grace window**: the period after a connected session drops during which the UI stays on the remote
  with a "reconnecting…" hint before escalating. A single tunable duration.
- **Connection phase (UI-facing)**: extends today's states so the UI can tell "briefly reconnecting
  (stay on remote)" apart from "really disconnected (show reconnect screen)".

## Success Criteria *(mandatory)*

- **SC-001**: After a lock/unlock with the TV still on, the remote is interactive again within **1
  second** of returning to the foreground, with **no** full-screen error shown.
- **SC-002**: A transient drop shorter than the grace window shows **zero** frames of the
  "Can't reach your TV" screen.
- **SC-003**: A reconnect after lock/unlock issues **no** pairing prompt (client-key reused).
- **SC-004**: When the TV is genuinely off, the app still escalates to the reconnect/off-network
  screen within the grace window + a bounded reconnect attempt — it never spins forever.
- **SC-005**: Backgrounded with a dead TV, the app makes **no** more than a bounded, backing-off
  number of attempts (no fixed 5s forever-loop).
- **SC-006**: Live volume/now-playing is not blanked during a sub-grace-window blip.

## Assumptions

- **Tier 1 has no foreground service**: the socket *does* drop while locked; this spec makes the
  *return* seamless rather than keeping the socket alive. True always-connected (no drop at all) is
  Tier 2 / `003`.
- **Same direct-LAN architecture as `002`**: secure SSAP socket, custom TrustManager, stored
  client-key; this spec changes connection *timing/lifecycle and UI routing*, not the protocol.
- **Lifecycle signal available**: the app can observe foreground/background (process lifecycle) to
  drive immediate-resume reconnect.
- **Android-only, home-Wi-Fi-only**: inherits `002`'s scope.

## Clarifications

### Open questions (for /clarify)

- **OQ1 — grace window length**: ~2s? ~3s? Long enough to hide a normal reconnect, short enough that a
  real outage is acknowledged quickly.
- **OQ2 — held-action-on-reconnect (FR-010)**: worth the risk of a late/unwanted command, or keep it
  simple and just let the user re-press once reconnected? (Recommendation: off for v1; revisit.)
- **OQ3 — backoff shape**: exact initial delay / factor / cap (e.g. 300ms → ×2 → cap 5s).
- **OQ4 — Tier 2 trigger**: should the foreground-service persistence be introduced *with* `003`
  (lockscreen) so locking keeps the socket alive and this spec's reconnect becomes a fallback, or kept
  as a separate opt-in later?
- **OQ5 — reconnecting affordance**: where/how subtle (a thin top shimmer? a small inline chip on the
  now-playing strip / connection area?) — must not block controls.
