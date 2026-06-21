# Theme colour generator — canonical M3 scheme for the Ultraviolet seed

`gen.mjs` emits the full light + dark Compose `ColorScheme` for the seed `#7B2FF7` using the Material
Theme Builder algorithm (`SchemeTonalSpot`, contrast 0). It's the reproducible source of truth for the
fallback scheme in `android/app/src/main/java/com/shakilclark/lgremote/ui/theme/Theme.kt`.

```bash
cd tools/theme
npm install            # one-time (material-dynamic-colors); node_modules is git-ignored
node gen.mjs           # full canonical Kotlin — both schemes
node gen.mjs json      # raw role -> hex (light & dark)
SEED='#RRGGBB' node gen.mjs   # try a different seed
```

## Tone policy (important)

Theme.kt deliberately **keeps the device-validated _visible_ tones** (primary / surface / containers /
outline — design-system §2.2/§2.3) rather than the raw canonical values, because a raw re-tone shifts
the approved look (e.g. canonical light primary `#7423F0` vs the validated `#6E2EC9`). Canonical values
are used only for the **non-visible roles** (error/error-container, inverse*, surfaceDim/Bright,
surfaceTint), which fixes off-palette Snackbars/error surfaces with no visual regression.

**To adopt the full canonical re-tone** (true MTB export): paste both `gen.mjs` blocks into Theme.kt
wholesale, then update `docs/design-system.md` §2.2/§2.3, the `tools/fidelity/refs/*.html` hex values,
and re-record the Roborazzi goldens (`./gradlew recordRoborazziDebug`).
