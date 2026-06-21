<!-- SPECKIT START -->
## Active feature: 002-native-android-remote

Native **Android** app (Kotlin + Jetpack Compose) that controls an LG webOS TV **directly over
the LAN** — no backend, no Tailscale, no HTTPS. Re-platforms `001`. Android-only, home-Wi-Fi-
only. Same user stories US1–US7 (pair, volume/mute, play/pause, D-pad nav, motion cursor,
YouTube/Netflix shortcuts, input switcher).

Read these for full context:
- Spec: `specs/002-native-android-remote/spec.md`
- Constitution (principles, non-negotiable, v1.1.0): `.specify/memory/constitution.md`
- Prior art (PWA + backend, protocol source of truth): `specs/001-webos-remote/`
  — esp. `contracts/tv-protocol.md` and the working `backend/` SSAP code (`lgtv2`-based).

Key decisions (from /clarify):
- Connect over **`wss://<tv-ip>:3001`** (secure SSAP); trust the TV's self-signed cert with a
  custom `TrustManager`. Do NOT rely on deprecated cleartext `ws://:3000`.
- **SSAP client written from scratch in Kotlin** (OkHttp WebSocket); Connect-SDK /
  heroslender/lg-remote are read-only references. Pairing client-key in app-private storage.
- **Manual IP entry first**; SSDP auto-discovery is a later slice. `minSdk 31`.
- Motion cursor uses native `SensorManager` (no HTTPS needed). Sideload signed APK (`adb`).
- UI designed **directly in Compose**; the `001` web UI is only a visual reference.
<!-- SPECKIT END -->

## Theming & UX work — always use the `material-3` skill

For **all** theming and UX/UI work on this app (colour/tokens, typography, shape, motion,
components, layout, Material You / dynamic colour, M3 Expressive), **always draw on the installed
`material-3` skill** (`~/.claude/skills/material-3`) — invoke `/material-3 [component|theme|layout|
scaffold|audit]` or let it auto-activate. This is the standing reference for Material 3 guidance.

It encodes **generic** M3, so on any conflict `docs/design-system.md` (our decided Direction-C /
Ultraviolet / native-finish design) is the **source of truth** and wins.
