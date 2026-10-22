package com.wireless.audiostreamer.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothPairingHelper(
    private val ctx: Context,
    private val adapter: BluetoothAdapter?
) {
    private var discoveryReceiver: BroadcastReceiver? = null
    private var bondReceiver: BroadcastReceiver? = null

    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        val a = adapter ?: return
        try { a.bondedDevices?.forEach(onDeviceFound) } catch (_: SecurityException) {}
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let(onDeviceFound)
            }
        }
        discoveryReceiver = receiver
        ctx.registerReceiver(receiver, filter)
        try {
            if (a.isDiscovering) a.cancelDiscovery()
            a.startDiscovery()
        } catch (_: SecurityException) { }
    }

    fun stopScan() {
        try { adapter?.cancelDiscovery() } catch (_: Exception) {}
        discoveryReceiver?.let { try { ctx.unregisterReceiver(it) } catch (_: Exception) {} }
        discoveryReceiver = null
    }

    fun pair(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            onResult(true); return
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val dev: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (dev?.address != device.address) return
                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)) {
                    BluetoothDevice.BOND_BONDED -> { onResult(true); cleanup() }
                    BluetoothDevice.BOND_NONE -> { onResult(false); cleanup() }
                }
            }
            fun cleanup() {
                try { ctx.unregisterReceiver(this) } catch (_: Exception) {}
                bondReceiver = null
            }
        }
        bondReceiver = receiver
        ctx.registerReceiver(receiver, filter)
        try {
            device.createBond()
        } catch (_: SecurityException) {
            onResult(false)
        }
    }
}
