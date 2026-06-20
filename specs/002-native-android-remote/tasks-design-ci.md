# Tasks: Design System + Test/CI/CD (002, post-US5)

Work to (a) apply the **M3 Expressive design system** (`docs/design-system.md`), (b) add a fast,
CI-friendly **visual-regression test** net (Roborazzi), and (c) stand up **CI/CD** that produces a
**wirelessly-installable signed APK**. All tooling is free / OSS.

**Key sequencing:** apply the design system *before* recording Roborazzi goldens (so the baseline is
the intended look, not the pre-redesign UI). Wireless dev loop and test infra can land first.

---

## Phase W — Wireless dev loop (quick win, do first)
- [ ] W1 Document `adb` **wireless debugging** (pair → `adb connect <ip>:port` → `./gradlew :app:installDebug` over Wi-Fi) in README + quickstart.
- [ ] W2 [P] (optional) Note a no-cable share fallback (Syncthing / `python -m http.server` of the APK dir). The CD release link (CD3) is the main share path.

## Phase D0 — Decision ✅
- [x] D0 Compose BOM: **bump to `2025.06.01`** (Material3 1.4.x — Expressive components + `MaterialShapes` morph). Opt into `@ExperimentalMaterial3ExpressiveApi` per-file. Do the bump right after W1 so test deps (Roborazzi/Robolectric) align to the final Compose version.

## Phase A — Design tokens (safe, no behaviour change; design-system §10.1)
- [ ] A1 Expand `ui/theme/Color.kt` to the full M3 role set; replace `DarkColors` in `Theme.kt` with the spec's `darkColorScheme` (keep forced-dark).
- [ ] A2 [P] Replace `AppTypography` in `Type.kt` with the emphasized scale (§3).
- [ ] A3 [P] Add `ui/theme/Shape.kt` (`AppShapes`) + wire `shapes` into the `MaterialTheme(...)` call (§4.1).
- [ ] A4 [P] Add `ui/theme/MotionSpecs.kt` (springs/tweens, §6) and `Space` spacing object (§5.2).

## Phase B — Shared primitive
- [ ] B1 `ui/components/PressMorph.kt` — `Modifier.pressMorph` (corner+scale spring, haptic, ripple off) (§4.2).
- [ ] B2 `ui/components/ControlKey.kt` — the workhorse 64dp morphing key (§7.1) + `@Preview`.

## Phase C — Recompose controls to spec (each: update + `@Preview`)
- [ ] C1 D-pad cluster: `extraLarge` panel, 64dp arrows, 80dp accented OK; per-key morph (§7.2).
- [ ] C2 [P] Volume/channel rockers + mute toggle, hold-to-repeat pulse (§7.3).
- [ ] C3 [P] Transport row: play/pause icon cross-fade + morph (§7.4).
- [ ] C4 [P] App shortcut chips — neutral fill, brand glyph only (§7.5).
- [ ] C5 Top-bar connection status: breathing live pulse; Pairing/Disconnected states; connection sheet (§7.6).
- [ ] C6 Input switcher bottom sheet with active-input check (§7.7).
- [ ] C7 Restyle `CursorPad` to spec + RemoteScreen layout/reach budget (§8); (optional) BOM bump + Expressive swaps if D0 chose it.

## Phase T — Test net (Robolectric + Roborazzi; JVM, no emulator)
- [ ] T1 Add Robolectric + **Roborazzi** to `libs.versions.toml` + `app` (plugin, deps, `roborazzi {}` / `testOptions { unitTests.isIncludeAndroidResources = true }`).
- [ ] T2 [P] Behaviour tests — `RemoteScreenTest` (ComposeTestRule + Robolectric): controls present, callbacks fire, controls rejected when not Connected. *(closes T045)*
- [ ] T3 Screenshot tests over the `@Preview`s / key components (ControlKey, DPad, rockers, CursorPad, RemoteScreen-connected, ConnectScreen, ConnectionBanner states). **Record goldens only after Phase C.**
- [ ] T4 [P] Commit goldens; document `recordRoborazziDebug` / `verifyRoborazziDebug` in quickstart.

## Phase CI — GitHub Actions (build + test)
- [ ] CI1 `.github/workflows/ci.yml` — Temurin 17, Gradle cache; run `testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug` on push + PR.
- [ ] CI2 [P] Upload debug APK + Roborazzi diff images as artifacts; add a CI status badge to README.

## Phase CD — Release + wireless install
- [ ] CD1 Release signing: generate keystore; store base64 keystore + key/store passwords as GitHub Secrets; wire `signingConfigs.release`. *(closes T046)*
- [ ] CD2 `.github/workflows/release.yml` — on `v*` tag: `assembleRelease`, sign, auto-bump `versionCode` from run number, attach signed APK to a GitHub **Release**.
- [ ] CD3 [P] Document install: open the Release link on the phone → tap APK → install (the over-the-air delivery).

---

### Suggested execution order
**W1 → T1 → T2 → A → B → C → T3/T4 (record goldens) → CI → CD.**
Tokens (A) are a safe first UI commit; behaviour tests (T2) work on the current UI; goldens (T3)
wait for the redesign to settle so the baseline is correct.
