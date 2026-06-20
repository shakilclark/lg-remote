package com.shakilclark.lgremote.cursor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class MotionCursorTest {

    /** Unit quaternion [w,x,y,z] for a rotation of [angle] rad about axis (x,y,z). */
    private fun quat(angle: Double, x: Double, y: Double, z: Double): DoubleArray {
        val h = angle / 2
        return doubleArrayOf(cos(h), x * sin(h), y * sin(h), z * sin(h))
    }

    private val identity = doubleArrayOf(1.0, 0.0, 0.0, 0.0)

    // --- dead-zone / scaling (T040) ---

    @Test
    fun `dead-zone suppresses jitter`() {
        assertEquals(0, CursorMath.axisDelta(0.0))
        assertEquals(0, CursorMath.axisDelta(CursorMath.DEAD_ZONE - 0.0001))
        assertEquals(0, CursorMath.axisDelta(-(CursorMath.DEAD_ZONE - 0.0001)))
    }

    @Test
    fun `motion past the dead-zone scales and keeps its sign`() {
        val d = CursorMath.axisDelta(0.02)
        assertTrue(d > 0)
        assertEquals(-d, CursorMath.axisDelta(-0.02))
    }

    @Test
    fun `a single frame is clamped to MAX_STEP`() {
        assertEquals(CursorMath.MAX_STEP, CursorMath.axisDelta(10.0))
        assertEquals(-CursorMath.MAX_STEP, CursorMath.axisDelta(-10.0))
    }

    // --- pan/tilt extraction (T039) ---

    @Test
    fun `no rotation yields no pan or tilt`() {
        val (pan, tilt) = CursorMath.panTilt(identity, identity)
        assertEquals(0.0, pan, 1e-9)
        assertEquals(0.0, tilt, 1e-9)
    }

    @Test
    fun `rotation about Y reads as pan, not tilt`() {
        val (pan, tilt) = CursorMath.panTilt(identity, quat(0.02, 0.0, 1.0, 0.0))
        assertEquals(0.02, pan, 1e-3) // ≈ angle for small rotations
        assertEquals(0.0, tilt, 1e-3)
    }

    @Test
    fun `rotation about X reads as tilt, not pan`() {
        val (pan, tilt) = CursorMath.panTilt(identity, quat(0.02, 1.0, 0.0, 0.0))
        assertEquals(0.02, tilt, 1e-3)
        assertEquals(0.0, pan, 1e-3)
    }

    @Test
    fun `delta is relative to the previous frame, not absolute`() {
        val a = quat(0.10, 0.0, 1.0, 0.0)
        val b = quat(0.12, 0.0, 1.0, 0.0)
        val (pan, _) = CursorMath.panTilt(a, b)
        assertEquals(0.02, pan, 1e-3) // only the 0.02 increment, not the absolute 0.12
    }

    // --- CursorFilter: smoothing + sub-pixel accumulation (cursor-smoothness fix) ---

    /** smoothing=1.0 disables the low-pass (pass-through), making the carry math exact to assert. */
    private fun rawFilter(sensitivity: Double = 1000.0, deadZone: Double = 0.0) =
        CursorFilter(sensitivity = sensitivity, deadZone = deadZone, smoothing = 1.0)

    @Test
    fun `sub-pixel motion is accumulated, never dropped`() {
        val f = rawFilter() // 1000 px/rad
        f.accumulate(0.0005, 0.0) // +0.5 px this frame
        assertEquals(0 to 0, f.drain()) // half a pixel → nothing yet, but it's kept
        f.accumulate(0.0005, 0.0) // another +0.5 px → 1.0 px banked
        assertEquals(1 to 0, f.drain()) // the two halves add up to one clean pixel
    }

    @Test
    fun `dead-zone suppresses sub-threshold jitter across many frames`() {
        val f = CursorFilter(sensitivity = 10_000.0, deadZone = 0.01, smoothing = 1.0)
        repeat(20) { f.accumulate(0.005, -0.005) } // each axis under the 0.01 dead-zone
        assertEquals(0 to 0, f.drain())
    }

    @Test
    fun `a flick is clamped per emit but the remainder still flows`() {
        val f = rawFilter(sensitivity = 1.0)
        f.accumulate(200.0, 0.0) // 200 px banked in one big sample
        assertEquals(CursorMath.MAX_STEP to 0, f.drain()) // clamped to 60…
        assertEquals(CursorMath.MAX_STEP to 0, f.drain()) // …rest isn't lost, keeps flowing
    }

    @Test
    fun `axis signs are preserved through the filter`() {
        val right = rawFilter(sensitivity = 10_000.0).apply { accumulate(0.02, 0.0) }.drain()
        val left = rawFilter(sensitivity = 10_000.0).apply { accumulate(-0.02, 0.0) }.drain()
        assertTrue(right.first > 0)
        assertEquals(-right.first, left.first)
    }

    @Test
    fun `reset clears accumulated motion`() {
        val f = rawFilter(sensitivity = 10_000.0)
        f.accumulate(0.02, 0.02)
        f.reset()
        assertEquals(0 to 0, f.drain())
    }

    @Test
    fun `low-pass ramps toward the input rather than jumping`() {
        // With smoothing<1 a sudden onset shouldn't reach full speed in a single frame.
        // Keep sensitivity low enough that neither value hits the MAX_STEP clamp (which would
        // mask the ramp): 0.02 rad * 1000 = 20 px instant, well under the 60 px clamp.
        val smooth = CursorFilter(sensitivity = 1000.0, deadZone = 0.0, smoothing = 0.3)
        smooth.accumulate(0.02, 0.0)
        val firstFrame = smooth.drain().first
        val instant = CursorMath.axisDelta(0.02, 1000.0, 0.0) // unsmoothed, same input
        assertTrue(firstFrame in 1 until instant) { "ramped $firstFrame should be below instant $instant" }
    }
}
