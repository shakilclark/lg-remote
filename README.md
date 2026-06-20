# LG webOS TV Remote

A native **Android** remote for an LG webOS TV that talks **directly to the TV over your home
Wi-Fi** — no backend, no cloud, no Tailscale, no HTTPS proxy. Kotlin + Jetpack Compose, built
spec-first with [GitHub Spec Kit](https://github.github.com/spec-kit/) — see
[`specs/002-native-android-remote/`](specs/002-native-android-remote/).

## How it works

The app opens a secure SSAP WebSocket straight to the TV at **`wss://<tv-ip>:3001`**, trusting
the TV's self-signed certificate with a custom `TrustManager`. The pairing client-key is kept in
app-private storage, so after a one-time "accept on your TV" prompt it reconnects silently. A
secondary pointer-input socket carries D-pad navigation and the motion cursor; the motion cursor
reads phone tilt via the native `SensorManager` — no web/HTTPS permission dance, which is the main
reason this is a native app rather than a PWA.

```
Android app ──wss:// SSAP (LAN, self-signed cert)──▶ LG webOS TV
```

Android-only, home-Wi-Fi-only, by design.

## Status

| Slice | Scope | State |
|-------|-------|-------|
| US1 | Pair once, auto-reconnect, live connection state | ✅ built |
| US2 | Volume / mute / play-pause (+ hardware volume rocker) | ✅ built |
| US3 | D-pad / OK / Back / Home / Exit | ✅ built |
| US6 | YouTube & Netflix shortcuts | ✅ built |
| US7 | Input switcher (HDMI / sources) | ✅ built |
| US5 | Motion (Magic Remote) cursor | ✅ built — on-device sensitivity tune pending |
| Resilience | Off-network detection · SSDP TV auto-discovery | ✅ built |
| Polish | App icon, release signing, full real-TV validation | ⏳ next |

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

Enter the TV's IP (or scan), accept the pairing prompt on the TV, and you're connected.

**No TV / no phone?** A debug-only `PreviewActivity` renders the full connected UI with sample
state, and works on an emulator:

```bash
adb shell am start -n com.shakilclark.lgremote/.PreviewActivity
```

## Sideload a shareable APK (no Play Store)

```bash
cd android
./gradlew :app:assembleRelease     # → app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
# or copy the APK to the phone and tap it (allow "install unknown apps" once)
```

No developer account required. Tests: `cd android && ./gradlew :app:testDebugUnitTest`
(SSAP client, commands, pointer + cursor math — all run against mocks, no TV needed).

## Project layout

- **`android/`** — the app (Kotlin + Jetpack Compose). The current product.
- **`specs/002-native-android-remote/`** — active spec, plan, tasks, quickstart.
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
