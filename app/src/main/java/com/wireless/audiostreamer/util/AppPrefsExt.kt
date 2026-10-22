package com.wireless.audiostreamer.util

import android.content.Context

/**
 * Extra prefs + auto-connect helper (same was_prefs_v4 store).
 * Uses SharedPreferences directly so AppPrefs.prefs stays private.
 */

private fun prefs(ctx: Context) =
    ctx.getSharedPreferences("was_prefs_v4", Context.MODE_PRIVATE)

fun AppPrefs.defaultBtAddress(ctx: Context): String? =
    prefs(ctx).getString("default_bt_address", null)

fun AppPrefs.setDefaultBtAddress(ctx: Context, address: String?) {
    prefs(ctx).edit().putString("default_bt_address", address).apply()
}

fun AppPrefs.defaultWifiTarget(ctx: Context): String? =
    prefs(ctx).getString("default_wifi_ip", null)

fun AppPrefs.setDefaultWifiTarget(ctx: Context, ip: String?) {
    prefs(ctx).edit().putString("default_wifi_ip", ip).apply()
}

/**
 * Auto-connect: default if present in list, else sole candidate, else null.
 */
fun <T> pickAutoConnectTarget(
    candidates: List<T>,
    idOf: (T) -> String,
    defaultId: String?
): T? {
    if (defaultId != null) {
        candidates.firstOrNull { idOf(it) == defaultId }?.let { return it }
    }
    return if (candidates.size == 1) candidates[0] else null
}
