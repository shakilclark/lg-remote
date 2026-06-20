# LG webOS TV Remote

[![CI](https://github.com/shakilclark/lg-remote/actions/workflows/ci.yml/badge.svg)](https://github.com/shakilclark/lg-remote/actions/workflows/ci.yml)

A native **Android** remote for an LG webOS TV that talks **directly to the TV over your home
Wi-Fi** — no backend, no cloud, no Tailscale, no HTTPS proxy. Kotlin + Jetpack Compose, built
spec-first with [GitHub Spec Kit](https://github.github.com/spec-kit/) — see
[`specs/002-native-android-remote/`](specs/002-native-android-remote/).

## How it works

The app opens a secure SSAP WebSocket straight to the TV at **`wss://<tv-ip>:3001`**, trusting
the TV's self-signed certificate with a custom `TrustManager`. The pairing client-key is kept in
app-private storage, so after a one-time "accept on your TV" prompt it reconnects silently. A
secondary pointer-input socket carries D-pad navigation, media transport, and an on-screen
**touchpad** cursor (drag to move the pointer, tap to click). Native access to the TV's self-signed
secure socket — which a browser refuses — is the main reason this is a native app, not a PWA.

```
Android app ──wss:// SSAP (LAN, self-signed cert)──▶ LG webOS TV
```

Android-only, home-Wi-Fi-only, by design.

## Status

| Slice | Scope | State |
|-------|-------|-------|
| US1 | Pair once, auto-reconnect, live connection state | ✅ built |
| US2 | Volume / mute / play-pause + transport (rewind / fast-forward) + hardware rocker | ✅ built |
| US3 | D-pad / OK + bottom bar (Back · Home · Apps · Inputs · Settings) | ✅ built |
| US5 | On-screen **touchpad** cursor (drag to move, tap to click) — replaces the gyro motion cursor | ✅ built |
| US6 | Dynamic app loader — all installed TV apps with icons, tap to launch | ✅ built |
| US7 | Input switcher (connected sources only) | ✅ built |
| 004 | Now-playing — live app + real play-state strip/sheet; play/pause bound to TV state | ✅ built |
| Settings | ⚙ launches the TV's own settings on-screen | ✅ built |
| Resilience | Off-network detection · TV auto-discovery (SSDP + port-3001 sweep) | ✅ built |
| Planned | Seamless reconnect (`008`) · lockscreen controls (`003`) · beautify (`005`) · audio output (`006`) · haptics (`007`) | 📋 specced |
| Polish | App icon, full real-TV validation | ⏳ next |

## Build & run (debug)

JDK 17+ and the Android SDK (or Android Studio). Phone with USB debugging on, on the **same
Wi-Fi** as the TV.

```bash
cd android
./gradlew :app:installDebug                                   # build + install to the phone
adb shell am start -n com.shakilclark.lgremote/.MainActivity  # launch
adb logcat -s LGRemote                                        # app logs
```

**No cable?** Use **adb wireless debugging** — phone → Developer options → *Wireless debugging* → on,
then `adb pair <phone-ip>:<port>` (enter the 6-digit code) and `adb connect <phone-ip>:<port>`; the
same `gradlew` commands then push over Wi-Fi. ([details](specs/002-native-android-remote/quickstart.md))

Tap **Scan for TVs** (or enter the TV's IP), accept the pairing prompt on the TV, and you're
connected.

**No TV / no phone?** The UI renders without one via Compose `@Preview` in Android Studio, and the
Roborazzi screenshot goldens in `app/src/test/screenshots/` show the current screens.

## Sideload a shareable APK (no Play Store)

```bash
cd android
./gradlew :app:assembleRelease     # → app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
# or copy the APK to the phone and tap it (allow "install unknown apps" once)
```

No developer account required. Tests (`cd android && ./gradlew :app:testDebugUnitTest`): SSAP
client, commands, pointer + cursor math, **Compose behaviour** (Robolectric), and **Roborazzi
screenshot / visual-regression** — all on the JVM, no TV or emulator. Re-record screenshot goldens
after intentional UI changes with `./gradlew :app:recordRoborazziDebug`.

## Project layout

- **`android/`** — the app (Kotlin + Jetpack Compose). The current product.
- **`specs/002-native-android-remote/`** — the shipped spec, plan, tasks, quickstart.
- **`specs/003…006/`** — planned/specced features: lockscreen controls, now-playing, beautify, audio output.
- **`docs/design-system.md`** — Material 3 Expressive UI spec (color/type/shape/motion tokens + component recipes).
- **`.specify/memory/constitution.md`** — project principles (v1.1.0, post-pivot).

### Prior art (the original PWA — kept as protocol reference, not the shipping product)

`001` was an installable **PWA + Node backend + Tailscale** design: a browser can't open the
TV's local socket from a secure context, so a Pi-hosted Node agent owned the socket and Tailscale
fronted it with HTTPS. We re-platformed to native Android for direct-LAN access and native sensors
(no agent, no HTTPS). The old code and its protocol notes remain useful references:

- `specs/001-webos-remote/` — the PWA spec, esp. `contracts/tv-protocol.md`.
- `backend/` (Node + `lgtv2`) — the SSAP behaviour, verified against the real TV.
- `frontend/` — the React PWA UI (visual reference only).
- `docs/setup-tailscale.md` — **legacy**, only relevant to running that old PWA.
