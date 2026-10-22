package com.wireless.audiostreamer.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Animated radar-style searching indicator (pulse rings + sweep).
 */
class RadarScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#4CAF50")
    }
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#334CAF50")
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    private var pulsePhase = 0f
    private var sweepAngle = 0f
    private var scanning = false

    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { pulsePhase = it.animatedValue as Float; invalidate() }
    }
    private val sweepAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2200
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { sweepAngle = it.animatedValue as Float; invalidate() }
    }

    fun startScanning() {
        if (scanning) return
        scanning = true
        visibility = VISIBLE
        pulseAnimator.start()
        sweepAnimator.start()
    }

    fun stopScanning() {
        scanning = false
        pulseAnimator.cancel()
        sweepAnimator.cancel()
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
        sweepAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = minOf(width, height) / 2f - 8f
        if (maxR <= 0) return

        for (i in 0..2) {
            val phase = (pulsePhase + i / 3f) % 1f
            val r = maxR * phase
            ringPaint.alpha = (255 * (1f - phase)).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, r, ringPaint)
        }

        canvas.save()
        canvas.rotate(sweepAngle, cx, cy)
        canvas.drawArc(
            cx - maxR, cy - maxR, cx + maxR, cy + maxR,
            0f, 35f, true, sweepPaint
        )
        canvas.restore()

        canvas.drawCircle(cx, cy, 10f, dotPaint)
    }
}
