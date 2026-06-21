<!-- SPECKIT START -->
<!-- Hand-maintained: 010+ were built slice-by-slice in-CLI with no specs/<feature>/plan.md, so the
     agent-context tool can't auto-target them (it would point at the last feature that had a plan, 002).
     Update this block by hand until a current plan.md exists. -->
## Active feature: 010-expressive-redesign (+ 020 shell-wiring polish)

Material 3 Expressive + Material You redesign ("Direction C") of the native Android LG webOS remote:
a calm gesture-pad home with a pull-up command sheet (Keypad / Apps / Inputs / Sound / Type), now-
playing bar + cover, Material You dynamic colour with an Ultraviolet fallback (light + dark). Shipped
and device-validated; now in polish + wiring the inactive shells (`specs/020-activate-shells`).

Read these for full context:
- **Design source of truth**: `docs/design-system.md` (+ rendered artifacts in `docs/design/`).
- Specs: `specs/010-expressive-redesign/spec.md`; shell-wiring backlog `specs/020-activate-shells/spec.md`.
- Constitution (principles, non-negotiable): `.specify/memory/constitution.md`.
- Underlying native-app architecture & TV protocol: `specs/002-native-android-remote/` — secure SSAP
  over `wss://<tv-ip>:3001` (custom `TrustManager`), from-scratch Kotlin SSAP/OkHttp client, motion
  cursor via `SensorManager`. Android-only, home-Wi-Fi-only, no backend.
- Fidelity harness (design parity): `tools/fidelity/` (`bash tools/fidelity/diff.sh`).

Build gate (Android Studio JBR, no system JDK):
`cd android && JAVA_HOME=<AS JBR> ./gradlew recordRoborazziDebug lintDebug assembleDebug --no-configuration-cache`
— grep for `BUILD SUCCESSFUL`; the config cache can mask a compile break behind a green exit code.
<!-- SPECKIT END -->

## Theming & UX work — always use the `material-3` skill

For **all** theming and UX/UI work on this app (colour/tokens, typography, shape, motion,
components, layout, Material You / dynamic colour, M3 Expressive), **always draw on the installed
`material-3` skill** (`~/.claude/skills/material-3`) — invoke `/material-3 [component|theme|layout|
scaffold|audit]` or let it auto-activate. This is the standing reference for Material 3 guidance.
