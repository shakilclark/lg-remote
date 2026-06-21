#!/usr/bin/env bash
# Machine-checked fidelity: render each design reference (full-bleed, Ultraviolet light) headlessly,
# then RMSE-compare against the matching Roborazzi golden. Lower RMSE = closer to the design.
# Usage: tools/fidelity/diff.sh   (run after `recordRoborazziDebug`)
set -u
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
ROOT="$HOME/dev/vibed/lg-remote"
REFS="$ROOT/tools/fidelity/refs"
OUT="$ROOT/tools/fidelity/out"
GOLD="$ROOT/android/app/src/test/screenshots"
mkdir -p "$OUT"

# surface  golden-basename  width  height   (height = the golden's true px — keep in sync)
SURFACES="
home remote_connected 411 1200
command-sheet command_sheet 411 488
cover now_playing_cover 411 345
app-grid app_grid 411 320
now-playing-bar now_playing_bar 411 56
"

echo "surface              RMSE (0=identical)   diff image"
echo "$SURFACES" | while read -r name golden w h; do
  [ -z "${name:-}" ] && continue
  ref="$REFS/$name.html"
  [ -f "$ref" ] || { echo "$name  (no reference)"; continue; }
  refpng="$OUT/$name.ref.png"
  diffpng="$OUT/$name.diff.png"
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --window-size="$w,$h" --screenshot="$refpng" "file://$ref" >/dev/null 2>&1
  magick "$refpng" -resize "${w}x${h}!" "$refpng" >/dev/null 2>&1
  rmse=$(compare -metric RMSE "$refpng" "$GOLD/$golden.png" "$diffpng" 2>&1)
  # structural (de-noised): blur + downscale washes out AA/font/glyph noise → layout/colour/shadow only
  rs="$OUT/$name.ref.s.png"; gs="$OUT/$name.gold.s.png"
  magick "$refpng"            -blur 0x2 -resize 25% "$rs" >/dev/null 2>&1
  magick "$GOLD/$golden.png"  -blur 0x2 -resize 25% "$gs" >/dev/null 2>&1
  srmse=$(compare -metric RMSE "$rs" "$gs" "$OUT/$name.diff.s.png" 2>&1)
  printf "%-16s structural=%-20s raw=%-20s\n" "$name" "$srmse" "$rmse"
done
