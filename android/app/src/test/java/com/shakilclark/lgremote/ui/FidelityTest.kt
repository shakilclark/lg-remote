package com.shakilclark.lgremote.ui

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt

/**
 * Design-fidelity gate (audit P2 — folds the old tools/fidelity diff.sh into the JVM test suite).
 * For each surface, RMSE-compares the recorded Roborazzi golden against the committed design reference
 * (the headless-Chrome render of tools/fidelity/refs/<surface>.html, checked in under
 * src/test/resources/fidelity/). Lower = closer to the decided design; the threshold catches real
 * layout/colour/shape drift while a box-downscale washes out font AA / glyph noise.
 *
 * Refresh refs after editing a reference HTML: `bash tools/fidelity/diff.sh` then copy
 * tools/fidelity/out/<surface>.ref.png -> src/test/resources/fidelity/<surface>.png. Goldens are
 * refreshed with `./gradlew recordRoborazziDebug` and must be current before this gate is meaningful.
 */
class FidelityTest {

    private data class Surface(val name: String, val golden: String, val threshold: Double)

    private val surfaces = listOf(
        Surface("home", "remote_connected", 0.07),
        Surface("command-sheet", "command_sheet", 0.07),
        Surface("cover", "now_playing_cover", 0.09),
        Surface("app-grid", "app_grid", 0.07),
        Surface("now-playing-bar", "now_playing_bar", 0.08),
    )

    @TestFactory
    fun designFidelity(): List<DynamicTest> = surfaces.map { s ->
        DynamicTest.dynamicTest("${s.name} matches the design reference") {
            val ref = loadResource("/fidelity/${s.name}.png")
            val golden = loadFile("src/test/screenshots/${s.golden}.png")
            require(ref.width == golden.width && ref.height == golden.height) {
                "${s.name}: ref ${ref.width}x${ref.height} != golden ${golden.width}x${golden.height}"
            }
            val rmse = structuralRmse(ref, golden)
            println("fidelity %-16s rmse=%.4f  (threshold %.2f)".format(s.name, rmse, s.threshold))
            check(rmse < s.threshold) {
                "${s.name}: design fidelity RMSE %.4f exceeds %.2f — golden has drifted from the ref"
                    .format(rmse, s.threshold)
            }
        }
    }

    private fun loadResource(path: String): BufferedImage =
        ImageIO.read(requireNotNull(javaClass.getResourceAsStream(path)) { "missing ref resource $path" })

    private fun loadFile(path: String): BufferedImage {
        val f = File(path)
        require(f.exists()) { "missing golden $path (run ./gradlew recordRoborazziDebug first)" }
        return ImageIO.read(f)
    }

    /** Box-downscale by [SCALE] (averages NxN blocks ~ blur+shrink) then normalized RMSE in 0..1. */
    private fun structuralRmse(a: BufferedImage, b: BufferedImage): Double {
        val w = a.width / SCALE
        val h = a.height / SCALE
        var sumSq = 0.0
        for (by in 0 until h) for (bx in 0 until w) {
            val (ar, ag, ab) = blockAvg(a, bx * SCALE, by * SCALE)
            val (br, bg, bb) = blockAvg(b, bx * SCALE, by * SCALE)
            sumSq += sq(ar - br) + sq(ag - bg) + sq(ab - bb)
        }
        return sqrt(sumSq / (w * h * 3)) / 255.0
    }

    private fun blockAvg(img: BufferedImage, x0: Int, y0: Int): Triple<Double, Double, Double> {
        var r = 0L; var g = 0L; var bl = 0L
        for (y in y0 until y0 + SCALE) for (x in x0 until x0 + SCALE) {
            val p = img.getRGB(x, y)
            r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; bl += p and 0xFF
        }
        val n = (SCALE * SCALE).toDouble()
        return Triple(r / n, g / n, bl / n)
    }

    private fun sq(d: Double) = d * d

    companion object {
        private const val SCALE = 4
    }
}
