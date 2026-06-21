# Fidelity harness — machine-checked design parity

Renders each design surface as a **full-bleed, Ultraviolet-light reference** at the golden's exact px
(headless Chrome), then RMSE-compares it to the Roborazzi golden. Lower RMSE = closer to the design.

```
./gradlew recordRoborazziDebug --no-configuration-cache   # refresh goldens
bash tools/fidelity/diff.sh                                # print RMSE + write out/<surface>.diff.png
```

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
