package com.shakilclark.lgremote.cursor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure, unit-testable cursor math (T039/T040). Converts frame-to-frame device rotation
 * (as quaternions) into LG pointer pixel deltas, with a dead-zone (jitter floor), sensitivity
 * scaling and a per-frame clamp. Working in *relative* (frame-to-frame) rotation avoids the
 * gimbal-lock you'd hit reading absolute orientation from an upright phone. Signs/scale are
 * tuned on the real TV (see the Phase 8 checkpoint).
 */
object CursorMath {
    /** Below this per-frame rotation (radians) we treat motion as jitter and emit nothing. */
    const val DEAD_ZONE = 0.0035

    /** Pixels of pointer travel per radian of device rotation. */
    const val SENSITIVITY = 1300.0

    /** Clamp a single frame's delta so a jerk can't fling the pointer. */
    const val MAX_STEP = 60

    /** Hamilton product a⊗b of two quaternions stored as [w, x, y, z]. */
    fun mul(a: DoubleArray, b: DoubleArray): DoubleArray = doubleArrayOf(
        a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3],
        a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
        a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1],
        a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0],
    )

    /** Conjugate — the inverse for a unit quaternion. */
    fun conj(q: DoubleArray): DoubleArray = doubleArrayOf(q[0], -q[1], -q[2], -q[3])

    /**
     * Small-angle rotation from [prev] to [curr], in the device frame, as (pan, tilt) radians —
     * pan = rotation about the device Y axis (left/right), tilt = about the device X axis
     * (up/down). For unit quaternions the vector part of the delta ≈ half the rotation angle
     * about each axis, so we double it.
     */
    fun panTilt(prev: DoubleArray, curr: DoubleArray): Pair<Double, Double> {
        val d = mul(conj(prev), curr)
        val pan = 2.0 * d[2]
        val tilt = 2.0 * d[1]
        return pan to tilt
    }

    /** One axis: rotation (rad) → pixel delta, after dead-zone, scaling and clamp. */
    fun axisDelta(rad: Double): Int = axisDelta(rad, SENSITIVITY, DEAD_ZONE)

    /** As [axisDelta], with explicit (live-tunable) sensitivity + dead-zone. */
    fun axisDelta(rad: Double, sensitivity: Double, deadZone: Double): Int {
        if (abs(rad) < deadZone) return 0
        return (rad * sensitivity).roundToInt().coerceIn(-MAX_STEP, MAX_STEP)
    }
}

/**
 * Live-tunable motion-cursor params (US5 on-device tuning). Read on the sensor thread, written from
 * the UI tuning panel, so fields are @Volatile. Defaults match [CursorMath].
 */
object CursorTuning {
    @Volatile var sensitivity: Double = CursorMath.SENSITIVITY
    @Volatile var deadZone: Double = CursorMath.DEAD_ZONE
    @Volatile var invertX: Boolean = false
    @Volatile var invertY: Boolean = false
}

/**
 * Drives the LG on-screen pointer from phone motion (US5 / T039, T041). Registers the
 * game-rotation-vector sensor (fallback rotation-vector) and, while [start]ed, emits (dx, dy)
 * pixel deltas via [onMove]. Motion is gated entirely by start/stop, so releasing the on-screen
 * button parks the cursor with no drift — US5 acceptance #3.
 */
class MotionCursor(
    private val sensorManager: SensorManager,
    private val onMove: (dx: Int, dy: Int) -> Unit,
) : SensorEventListener {

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val q = FloatArray(4)
    private var last: DoubleArray? = null

    /** True if the device has a usable rotation sensor. */
    val available: Boolean get() = sensor != null

    fun start() {
        last = null
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        last = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getQuaternionFromVector(q, event.values)
        val curr = doubleArrayOf(q[0].toDouble(), q[1].toDouble(), q[2].toDouble(), q[3].toDouble())
        val prev = last
        last = curr
        if (prev == null) return // first sample only establishes the reference frame
        val (pan, tilt) = CursorMath.panTilt(prev, curr)
        val s = CursorTuning.sensitivity
        val dz = CursorTuning.deadZone
        var dx = CursorMath.axisDelta(pan, s, dz) // pan right → cursor right
        var dy = CursorMath.axisDelta(-tilt, s, dz) // tilt up → cursor up
        if (CursorTuning.invertX) dx = -dx
        if (CursorTuning.invertY) dy = -dy
        if (dx != 0 || dy != 0) onMove(dx, dy)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
