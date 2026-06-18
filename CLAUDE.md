<!-- SPECKIT START -->
## Active feature: 001-webos-remote

LG webOS TV remote — an installable PWA (React + TS + Vite) backed by a Node + TS service on
an always-on Raspberry Pi that holds the TV's SSAP WebSocket (`lgtv2`) and is exposed over
HTTPS via Tailscale Serve. MVP = pairing, volume/mute, play/pause, D-pad navigation.

Read these for full context:
- Plan: `specs/001-webos-remote/plan.md`
- Spec: `specs/001-webos-remote/spec.md`
- Research / decisions: `specs/001-webos-remote/research.md`
- Data model: `specs/001-webos-remote/data-model.md`
- Contracts: `specs/001-webos-remote/contracts/` (backend-api.md, tv-protocol.md)
- Constitution (principles, non-negotiable): `.specify/memory/constitution.md`

Key rule: the browser never talks to the TV directly (mixed-content); the backend owns the
only `ws://` socket to the TV and the pairing key lives server-side in `~/.config/lg-remote/`.
<!-- SPECKIT END -->
