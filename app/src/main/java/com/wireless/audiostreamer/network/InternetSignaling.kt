package com.wireless.audiostreamer.network

import android.content.Context

/**
 * Internet location / peer exchange signaling.
 * Full implementation will be restored from source zip.
 */
class InternetSignaling(
    private val ctx: Context,
    private val pin: String,
    private val deviceName: String,
    private val role: String,
    private val onPeer: (String, String) -> Unit
) {
    fun start() {}
    fun stop() {}
    fun updateLocalLocation(lat: Double, lon: Double) {}
}
