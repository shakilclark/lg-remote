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
}
