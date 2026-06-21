// Canonical Material 3 colour-scheme generator for the LG Remote.
//
// Emits the full light + dark ColorScheme for the Ultraviolet seed using the Material Theme Builder
// algorithm (SchemeTonalSpot, contrast 0), via `material-dynamic-colors`. This is the reproducible
// source of truth for the fallback scheme in ui/theme/Theme.kt.
//
// Usage:
//   cd tools/theme && npm install        # one-time
//   node gen.mjs            > scheme.kt   # full canonical Kotlin (both schemes)
//   node gen.mjs json       > scheme.json # raw role->hex for light & dark
//
// Policy (see Theme.kt header): the app currently KEEPS the device-validated visible tones
// (primary/surface/containers/outline from design-system §2.2/§2.3) and uses canonical values only
// for the non-visible roles (error/inverse/surfaceDim/Bright/surfaceTint). To adopt the full
// canonical re-tone instead, paste both blocks below into Theme.kt wholesale, then update
// docs/design-system.md §2.2/§2.3, the tools/fidelity refs, and re-record the Roborazzi goldens.

import mdcDefault from "material-dynamic-colors";
const mdc = mdcDefault.default || mdcDefault;

const SEED = process.env.SEED || "#7B2FF7";

// Order mirrors a Material Theme Builder Compose export.
const ROLES = [
  "primary","onPrimary","primaryContainer","onPrimaryContainer",
  "secondary","onSecondary","secondaryContainer","onSecondaryContainer",
  "tertiary","onTertiary","tertiaryContainer","onTertiaryContainer",
  "error","onError","errorContainer","onErrorContainer",
  "background","onBackground","surface","onSurface","surfaceVariant","onSurfaceVariant",
  "surfaceDim","surfaceBright",
  "surfaceContainerLowest","surfaceContainerLow","surfaceContainer","surfaceContainerHigh","surfaceContainerHighest",
  "outline","outlineVariant","inverseSurface","inverseOnSurface","inversePrimary","scrim",
];

const theme = await mdc(SEED);

function kt(name, fn, m) {
  const lines = ROLES
    .filter((r) => m[r])
    .map((r) => `    ${r} = Color(0xFF${m[r].slice(1).toUpperCase()}),`);
  // surfaceTint defaults to primary by M3 convention; emit it explicitly.
  lines.push(`    surfaceTint = Color(0xFF${m.primary.slice(1).toUpperCase()}),`);
  return `val ${name} = ${fn}(\n${lines.join("\n")}\n)`;
}

if (process.argv[2] === "json") {
  console.log(JSON.stringify({ seed: SEED, light: theme.light, dark: theme.dark }));
} else {
  console.log(`// Generated from seed ${SEED} via the Material Theme Builder algorithm (SchemeTonalSpot, contrast 0).`);
  console.log(`// Do not hand-edit; regenerate with: node tools/theme/gen.mjs`);
  console.log("");
  console.log(kt("UltravioletLight", "lightColorScheme", theme.light));
  console.log("");
  console.log(kt("UltravioletDark", "darkColorScheme", theme.dark));
}
