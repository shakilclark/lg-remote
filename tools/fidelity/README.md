# Fidelity harness — machine-checked design parity

Each design surface has a **full-bleed, Ultraviolet-light reference** (`refs/<surface>.html`) rendered at
the golden's exact px (headless Chrome). The reference is RMSE-compared to the Roborazzi golden; lower =
closer to the design.

**The gate is a JVM test now** — `FidelityTest` (`android/app/src/test/...`) RMSE-compares each golden
against the committed reference PNG and fails on drift, so design parity runs in CI with the rest of the
suite. `diff.sh` is kept for **re-rendering the reference PNGs** and ad-hoc visual diffs.

```
# normal gate (parity runs as part of the unit tests):
./gradlew recordRoborazziDebug   # refresh goldens after a UI change
./gradlew testDebugUnitTest --tests '*FidelityTest*'   # check parity

# after editing a refs/<surface>.html — re-render + recommit the reference PNG:
bash tools/fidelity/diff.sh      # renders tools/fidelity/out/<surface>.ref.png (+ prints ImageMagick RMSE)
cp tools/fidelity/out/<surface>.ref.png android/app/src/test/resources/fidelity/<surface>.png
```

The committed references the test reads live in `android/app/src/test/resources/fidelity/<surface>.png`
(one per `diff.sh` surface row); keep them in sync when a `refs/<surface>.html` changes.

## What the refs are

`refs/<surface>.html` encodes the **design-as-decided**, not a raw mockup trace:

- Ported from the source-of-truth artifacts in [`docs/design/`](../../docs/design/) using their
  **literal light-mode CSS** (the dynamic-off Ultraviolet fallback the goldens render on).
- Reflecting the **documented deviations** from those mockups — e.g. VOL edge only (channel dropped),
  bottom-left Back corner, greyed inactive shells per spec 020, and the native-M3 finish (rounded-square
  keys, standard components) the [design-system fidelity stance](../../docs/design-system.md) calls for.
- Authored on **surface `#FDF7FF`** to match the golden background. The screenshot harness wraps each
  surface in a surface-coloured `Surface` (see `ScreenshotTest.shot`), so goldens render the real
  on-device fallback background rather than the bare Robolectric `#FAFAFA` default.

So RMSE measures genuine drift between the shipped app and the decided design — not intentional choices.
Track the **trend down** as surfaces are tightened.

## Surfaces measured

| ref | golden | px |
|---|---|---|
| `home.html` | `remote_connected` | 411×1200 |
| `command-sheet.html` | `command_sheet` | 411×472 |
| `cover.html` | `now_playing_cover` | 411×345 |
| `app-grid.html` | `app_grid` | 411×320 |
| `now-playing-bar.html` | `now_playing_bar` | 411×56 |

## Notes

- `out/<surface>.diff.png` — red = differing pixels; bright = biggest drift. (git-ignored)
- Two metrics: **raw** RMSE and **structural** (blur 0x2 + 25% downscale) which washes out font
  anti-aliasing and emoji-vs-Material-Symbol glyph noise, leaving layout/colour/shadow. Trust structural.
- Residual drift to expect: letter-tile fallbacks (the goldens have no network, so app icons render as
  letters) and AA on glyph edges.
- Add a surface: author `refs/<name>.html` (full-bleed, UV light, on `#FDF7FF`, the golden's exact px)
  + a row in `diff.sh` (`<name> <golden-basename> <w> <h>`). Keep the `<w> <h>` in sync with the golden.
