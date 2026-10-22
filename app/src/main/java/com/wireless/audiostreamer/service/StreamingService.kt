package com.wireless.audiostreamer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Temporary compile stub — full original StreamingService.kt from source zip
 * will replace this in the next commit without any logic change.
 */
class StreamingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    companion object {
        const val ACTION_STOP_RECORDING = "com.wireless.audiostreamer.STOP_RECORDING"
    }
}
