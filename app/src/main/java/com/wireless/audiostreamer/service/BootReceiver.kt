package com.wireless.audiostreamer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.wireless.audiostreamer.util.AppPrefs

/**
 * فون restart / ایپ اپڈیٹ کے بعد Sender سروس خود چلے —
 * UI کھولے بغیر Receiver کنٹرول کر سکے۔
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val ok = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_USER_PRESENT
        if (!ok) return

        if (!AppPrefs.isSetupDone(context)) return
        // Sender always-on: UI کھلے بغیر listening
        if (AppPrefs.role(context) != AppPrefs.ROLE_SENDER) return
        if (!AppPrefs.senderAlwaysOn(context)) return

        Log.i(TAG, "Auto-start Sender service action=$action")
        startSender(context)
    }

    companion object {
        private const val TAG = "BootReceiver"

        fun startSender(context: Context) {
            val i = Intent(context, StreamingService::class.java).apply {
                this.action = "sender_listen"
                putExtra("role", AppPrefs.ROLE_SENDER)
                putExtra("pin", AppPrefs.pin(context))
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startSender failed", e)
            }
        }
    }
}
