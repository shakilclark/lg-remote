# Quickstart — Native Android Remote

How to build, run, and sideload the app, plus the per-slice validation log against the real TV.

## Prerequisites

- **Android Studio** (latest stable) with the Android SDK, or just the command-line SDK +
  `gradle`. JDK 17.
- An Android phone with **USB debugging** on (Settings → Developer options), connected via USB,
  or `adb` over Wi-Fi. Phone and TV on the **same Wi-Fi**.
- The LG TV on the network. Known address from `001`: **192.168.0.9** (manual-IP path for v1).

## Build & run (debug)

```bash
cd android
./gradlew :app:installDebug      # builds + installs the debug APK to the connected phone
# or open android/ in Android Studio and Run ▶
adb shell am start -n com.shakilclark.lgremote/.MainActivity   # launch
adb logcat -s LGRemote                                          # app logs
```

### Wireless debugging (no cable)

Build + install over Wi-Fi — same `gradlew` commands, no USB:

1. Phone → Developer options → **Wireless debugging** → on.
2. Pair once (Wireless debugging → *Pair device with pairing code*):
   ```bash
   adb pair <phone-ip>:<pair-port>      # enter the 6-digit code shown on the phone
   adb connect <phone-ip>:<debug-port>  # the port under "IP address & Port"
   ```
3. `adb devices` shows the phone — now `installDebug`, launch, and logcat all work wirelessly.

No-cable share without `adb`: sync the APK to the phone via **Syncthing** (OSS) and tap it, or
`python3 -m http.server` in the APK dir and open `http://<mac-ip>:8000/<apk>` on the phone. The CD
release link (below) is the main share path.

## Sideload a shareable APK (no Play Store)

```bash
cd android
./gradlew :app:assembleRelease   # outputs app/build/outputs/apk/release/app-release.apk
# sign once with a local keystore (debug signing is auto for installDebug):
#   keytool -genkey -v -keystore ~/.android/lgremote.jks -alias lgremote -keyalg RSA -validity 10000
# then either:
adb install -r app/build/outputs/apk/release/app-release.apk
# or copy the APK to the phone and tap it (allow "install unknown apps" once).
```
No developer account required (Principle II / FR-011).

## Manual validation log (per slice — Principle V)

Each slice is "done" only when checked on the real phone + TV. Record results here.

| Slice | What to verify | TV (192.168.0.9) | Status |
|-------|----------------|------------------|--------|
| US1 Pair | Fresh install → enter IP → accept prompt on TV → "Connected"; kill+reopen → silent reconnect | wss://:3001 cert trusted, client-key persisted | ☐ |
| US2 Volume/PlayPause | Vol +/- moves TV bar & app level; mute toggles; play/pause toggles media; HW volume rocker drives TV | | ☐ |
| US3 D-pad | Up/Down/Left/Right move focus; OK selects; Back/Home work | pointer socket | ☐ |
| US6 Shortcuts | YouTube launches; Netflix launches; not-installed → clear message | | ☐ |
| US7 Inputs | Input list shows labels (e.g. HDMI_2 "PS4…"); tap switches source | | ☐ |
| US5 Cursor | Hold + move phone → LG pointer tracks; tap clicks; release → no drift (retune sensitivity) | SensorManager, no HTTPS | ☐ |
| Resilience | TV off → Disconnected state + retry; phone off-Wi-Fi → OffNetwork; nothing silently dropped | | ☐ |

## Notes

- If discovery is added later: needs a `WifiManager.MulticastLock`; until then manual IP is the
  path.
- On `targetSdk 37+` (Android 17): the app will request `ACCESS_LOCAL_NETWORK` on first connect;
  if denied, expect the `PermissionRequired` state with a link to settings (FR-017).
- The `001` `backend/` is the protocol reference if the TV behaves unexpectedly (it was verified
  against this exact TV).
