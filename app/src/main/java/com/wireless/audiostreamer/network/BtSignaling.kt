package com.wireless.audiostreamer.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.wireless.audiostreamer.util.AppPrefs
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bluetooth IP exchange: secure RFCOMM, connect timeout, last device first.
 */
class BtSignaling(
    private val ctx: Context,
    private val pin: String,
    private val deviceName: String,
    private val role: String,
    private val onPeer: (String, String, String) -> Unit,
    private val onStatus: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "BtSignaling"
        val SERVICE_UUID: UUID = UUID.fromString("7a1e5c02-4b7e-4c55-9f10-0d2f3a6b9c11")
        private const val CONNECT_TIMEOUT_MS = 8_000L
        private const val RETRY_COOLDOWN_MS = 15_000L
        private const val SCAN_PASS_DELAY_MS = 5_000L
    }

    private val adapter: BluetoothAdapter? =
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val running = AtomicBoolean(false)
    private val pool: ExecutorService = Executors.newCachedThreadPool()
    private val tried = ConcurrentHashMap<String, Long>()
    private var serverSocket: android.bluetooth.BluetoothServerSocket? = null

    private fun hasConnectPerm(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun isAvailable(): Boolean {
        val a = adapter ?: run {
            onStatus?.invoke("no_adapter"); return false
        }
        if (!hasConnectPerm()) {
            onStatus?.invoke("no_permission"); return false
        }
        if (!a.isEnabled) {
            onStatus?.invoke("bluetooth_off"); return false
        }
        return true
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        pool.execute { acceptLoop() }
        pool.execute { dialLoop() }
    }

    fun stop() {
        running.set(false)
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    private fun sleepQuiet(ms: Long) {
        try { Thread.sleep(ms) } catch (_: InterruptedException) {}
    }

    private fun acceptLoop() {
        while (running.get()) {
            if (!isAvailable()) { sleepQuiet(4000); continue }
            try {
                val server = adapter!!.listenUsingRfcommWithServiceRecord("WirelessAudioStreamer", SERVICE_UUID)
                serverSocket = server
                while (running.get()) {
                    val socket = server.accept()
                    pool.execute { handleSession(socket) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "accept loop error: ${e.message}")
                try { serverSocket?.close() } catch (_: Exception) {}
                sleepQuiet(2000)
            }
        }
    }

    private fun candidateDevices(): List<BluetoothDevice> {
        val bonded = try { adapter?.bondedDevices ?: emptySet() } catch (_: SecurityException) { emptySet() }
        if (bonded.isEmpty()) return emptyList()
        val lastAddr = AppPrefs.lastBtDeviceAddress(ctx)
        return bonded.sortedBy { dev ->
            if (dev.address == lastAddr) 0 else 1
        }
    }

    private fun dialLoop() {
        while (running.get()) {
            if (!isAvailable()) { sleepQuiet(SCAN_PASS_DELAY_MS); continue }
            val devices = candidateDevices()
            for (device in devices) {
                if (!running.get()) break
                val last = tried[device.address] ?: 0L
                if (System.currentTimeMillis() - last < RETRY_COOLDOWN_MS) continue
                tried[device.address] = System.currentTimeMillis()

                try {
                    adapter?.cancelDiscovery()
                    val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                    connectWithTimeout(socket, CONNECT_TIMEOUT_MS)
                    AppPrefs.setLastBtDeviceAddress(ctx, device.address)
                    handleSession(socket)
                } catch (e: TimeoutException) {
                    Log.d(TAG, "connect timeout: ${device.address}")
                } catch (e: Exception) {
                    Log.d(TAG, "connect failed: ${device.address} (${e.message})")
                }
            }
            sleepQuiet(SCAN_PASS_DELAY_MS)
        }
    }

    private fun connectWithTimeout(socket: BluetoothSocket, timeoutMs: Long) {
        val f = pool.submit { socket.connect() }
        try {
            f.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
            f.cancel(true)
            throw e
        }
    }

    private fun handleSession(socket: BluetoothSocket) {
        socket.use { s ->
            val input = DataInputStream(s.inputStream)
            val output = DataOutputStream(s.outputStream)
            val localIp = NetUtil.localIps().firstOrNull { NetUtil.isPrivateIp(it) } ?: ""
            val hello = "$localIp|$pin|$deviceName|$role"
            output.writeUTF(hello)
            output.flush()
            val remoteHello = input.readUTF()
            val parts = remoteHello.split("|")
            if (parts.size >= 3) {
                onPeer(parts[0], parts[2], parts.getOrElse(3) { "" })
            }
        }
    }
}
