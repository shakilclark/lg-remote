# Fidelity harness — machine-checked design parity

Renders each design surface as a **full-bleed, Ultraviolet-light reference** at the golden's exact px
(headless Chrome), then RMSE-compares it to the Roborazzi golden. Lower RMSE = closer to the design.

```
./gradlew recordRoborazziDebug --no-configuration-cache   # refresh goldens
bash tools/fidelity/diff.sh                                # print RMSE + write out/<surface>.diff.png
```

- `refs/<surface>.html` — the design at the Compose canvas size, literal CSS values (the spec).
- `out/<surface>.diff.png` — red = differing pixels; bright = biggest drift. (git-ignored)
- Noise to ignore: font hinting/AA, and emoji-vs-Material-Symbol icon pixels — match icons in the ref
  to reduce false drift. Track the **trend** of RMSE down as surfaces are ported to the literal CSS.

Add a surface: author `refs/<name>.html` (full-bleed, UV light) + a row in `diff.sh`
(`<name> <golden-basename> <w> <h>`).
