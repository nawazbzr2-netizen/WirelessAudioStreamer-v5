package com.wireless.audiostreamer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Injects remote taps received from Receiver.
 * User must enable this service in Settings → Accessibility.
 */
class RemoteControlAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Remote control accessibility connected")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun performTap(normX: Float, normY: Float) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val x = normX * metrics.widthPixels
        val y = normY * metrics.heightPixels

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Tap at $x,$y")
    }

    companion object {
        private const val TAG = "RemoteA11y"
        @Volatile var instance: RemoteControlAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null

        fun tap(normX: Float, normY: Float) {
            instance?.performTap(normX, normY)
        }
    }
}
