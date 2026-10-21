package com.wireless.audiostreamer

import android.app.Application
import android.util.Log
import com.wireless.audiostreamer.service.BootReceiver
import com.wireless.audiostreamer.util.AppPrefs

/**
 * ایپ کا پروسیس شروع ہوتے ہی (UI کھلے بغیر بھی)
 * Sender سروس چالو — Receiver کنٹرول کر سکے۔
 */
class WasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (AppPrefs.isSetupDone(this) &&
                AppPrefs.role(this) == AppPrefs.ROLE_SENDER &&
                AppPrefs.senderAlwaysOn(this)
            ) {
                Log.i("WasApp", "Process start → Sender service")
                BootReceiver.startSender(this)
            }
        } catch (e: Exception) {
            Log.w("WasApp", "auto start: ${e.message}")
        }
    }
}
