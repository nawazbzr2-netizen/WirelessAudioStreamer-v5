package com.wireless.audiostreamer.util

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {
    const val ROLE_SENDER = "SENDER"
    const val ROLE_RECEIVER = "RECEIVER"

    const val MODE_LOW_LATENCY = 0
    const val MODE_BALANCED = 1
    const val MODE_HIGH_QUALITY = 2

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("was_prefs_v4", Context.MODE_PRIVATE)

    fun role(ctx: Context): String =
        prefs(ctx).getString("role", ROLE_RECEIVER) ?: ROLE_RECEIVER

    fun setRole(ctx: Context, role: String) =
        prefs(ctx).edit().putString("role", role).apply()

    fun pin(ctx: Context): String =
        prefs(ctx).getString("pin", "1234") ?: "1234"

    fun setPin(ctx: Context, pin: String) =
        prefs(ctx).edit().putString("pin", pin).apply()

    fun deviceName(ctx: Context? = null): String {
        if (ctx == null) return android.os.Build.MODEL
        return prefs(ctx).getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL
    }

    fun setDeviceName(ctx: Context, name: String) =
        prefs(ctx).edit().putString("device_name", name).apply()

    fun isStereo(ctx: Context): Boolean =
        prefs(ctx).getBoolean("stereo", false)

    fun setStereo(ctx: Context, stereo: Boolean) =
        prefs(ctx).edit().putBoolean("stereo", stereo).apply()

    fun channels(ctx: Context): Int = if (isStereo(ctx)) 2 else 1

    fun sampleRate(ctx: Context): Int =
        prefs(ctx).getInt("sample_rate", 48000)

    fun setSampleRate(ctx: Context, rate: Int) =
        prefs(ctx).edit().putInt("sample_rate", if (rate >= 48000) 48000 else 16000).apply()

    fun bitrateKbps(ctx: Context): Int =
        prefs(ctx).getInt("bitrate_kbps", 128)

    fun setBitrateKbps(ctx: Context, kbps: Int) =
        prefs(ctx).edit().putInt("bitrate_kbps", kbps.coerceIn(32, 320)).apply()

    fun mode(ctx: Context): Int =
        prefs(ctx).getInt("mode", MODE_BALANCED)

    fun setMode(ctx: Context, mode: Int) =
        prefs(ctx).edit().putInt("mode", mode.coerceIn(0, 2)).apply()

    fun targetFrames(ctx: Context): Int =
        when (mode(ctx)) {
            MODE_LOW_LATENCY -> 2
            MODE_HIGH_QUALITY -> 8
            else -> 4
        }

    fun manualIp(ctx: Context): String =
        prefs(ctx).getString("manual_ip", "") ?: ""

    fun setManualIp(ctx: Context, ip: String) =
        prefs(ctx).edit().putString("manual_ip", ip).apply()

    fun isSetupDone(ctx: Context): Boolean =
        prefs(ctx).getBoolean("setup_done", false)

    fun setSetupDone(ctx: Context, done: Boolean = true) =
        prefs(ctx).edit().putBoolean("setup_done", done).apply()

    fun multiReceiverEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean("multi_receiver", true)

    fun setMultiReceiverEnabled(ctx: Context, enabled: Boolean) =
        prefs(ctx).edit().putBoolean("multi_receiver", enabled).apply()

    fun adaptiveBitrate(ctx: Context): Boolean =
        prefs(ctx).getBoolean("adaptive", true)

    fun setAdaptiveBitrate(ctx: Context, enabled: Boolean) =
        prefs(ctx).edit().putBoolean("adaptive", enabled).apply()

    fun lastPeerIp(ctx: Context): String =
        prefs(ctx).getString("last_peer_ip", "") ?: ""

    fun lastPeerName(ctx: Context): String =
        prefs(ctx).getString("last_peer_name", "") ?: ""

    fun hasSavedSession(ctx: Context): Boolean =
        lastPeerIp(ctx).isNotEmpty()

    fun saveSession(ctx: Context, ip: String, name: String) {
        prefs(ctx).edit()
            .putString("last_peer_ip", ip)
            .putString("last_peer_name", name)
            .putString("manual_ip", ip)
            .putBoolean("session_active", true)
            .apply()
    }

    fun clearSession(ctx: Context) {
        prefs(ctx).edit()
            .putString("last_peer_ip", "")
            .putString("last_peer_name", "")
            .putBoolean("session_active", false)
            .apply()
    }

    fun isSessionActive(ctx: Context): Boolean =
        prefs(ctx).getBoolean("session_active", false)

    fun senderAlwaysOn(ctx: Context): Boolean =
        prefs(ctx).getBoolean("sender_always_on", true)

    fun setSenderAlwaysOn(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean("sender_always_on", on).apply()

    /** Last Bluetooth device that connected successfully (MAC). */
    fun lastBtDeviceAddress(ctx: Context): String =
        prefs(ctx).getString("last_bt_addr", "") ?: ""

    fun setLastBtDeviceAddress(ctx: Context, address: String) =
        prefs(ctx).edit().putString("last_bt_addr", address).apply()
}
