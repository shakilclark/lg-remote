package com.shakilclark.lgremote

import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guard (audit P3): no hard-coded ARGB colour literals (`Color(0x…)`) outside the `theme/` package —
 * every colour must route through `MaterialTheme.colorScheme` / the theme tokens. Locks in the
 * colour-SSOT work (the retired legacy electric-red `Color.kt`) so a brand-palette island can't creep
 * back. Named system colours (`Color.White/Black/Transparent`) are intentionally allowed: they're used
 * for documented draw-layer overlays (e.g. the gesture pad's recessed-well shadow), not brand colour.
 */
class HardcodedColorTest {

    @Test
    fun noHardcodedArgbColorsOutsideTheme() {
        val root = File("src/main/java/com/shakilclark/lgremote")
        check(root.isDirectory) { "source root not found from ${File(".").absolutePath}" }
        val rx = Regex("""Color\(0x""")
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { "/theme/" in it.path.replace(File.separatorChar, '/') }
            .flatMap { f ->
                f.readLines().mapIndexedNotNull { i, line ->
                    if (rx.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null
                }
            }
            .toList()
        check(offenders.isEmpty()) {
            "Hard-coded Color(0x…) outside theme/ — route through colorScheme / theme tokens:\n" +
                offenders.joinToString("\n")
        }
    }
}
