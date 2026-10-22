package com.wireless.audiostreamer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetUtil {
    private val IPV4 = Regex("""^\\d{1,3}(\\.\\d{1,3}){3}$""")

    fun isPrivateIp(ip: String?): Boolean {
        if (ip == null || !IPV4.matches(ip)) return false
        return try {
            val a = InetAddress.getByName(ip)
            a is Inet4Address && !a.isLoopbackAddress && (a.isSiteLocalAddress || a.isLinkLocalAddress)
        } catch (e: Exception) {
            false
        }
    }

    fun localIps(): List<String> {
        val out = ArrayList<String>()
        try {
            val all = NetworkInterface.getNetworkInterfaces() ?: return out
            for (ni in all.toList()) {
                if (!ni.isUp || ni.isLoopback) continue
                for (ia in ni.interfaceAddresses) {
                    val a = ia.address
                    if (a is Inet4Address && (a.isSiteLocalAddress || a.isLinkLocalAddress)) {
                        a.hostAddress?.let { out.add(it) }
                    }
                }
            }
        } catch (e: Exception) {}
        return out
    }

    fun broadcastAddresses(): List<InetAddress> {
        val out = LinkedHashSet<InetAddress>()
        try {
            val all = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (ni in all.toList()) {
                if (!ni.isUp || ni.isLoopback) continue
                for (ia in ni.interfaceAddresses) {
                    val a = ia.address
                    val b = ia.broadcast
                    if (b != null && a is Inet4Address && (a.isSiteLocalAddress || a.isLinkLocalAddress)) {
                        out.add(b)
                    }
                }
            }
            try { out.add(InetAddress.getByName("192.168.43.255")) } catch (_: Exception) {}
            try { out.add(InetAddress.getByName("192.168.137.255")) } catch (_: Exception) {}
            try { out.add(InetAddress.getByName("192.168.49.255")) } catch (_: Exception) {}
        } catch (e: Exception) {}
        return out.toList()
    }

    @Suppress("DEPRECATION")
    fun bindToWifi(ctx: Context, s: DatagramSocket) {
        try {
            val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return
            for (n in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(n) ?: continue
                val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val vpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                if (wifi && !vpn) {
                    try {
                        n.bindSocket(s)
                        return
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {}
    }

    fun isHotspotLikely(): Boolean {
        return localIps().any {
            it.startsWith("192.168.43.") || it.startsWith("192.168.137.") ||
                it.startsWith("172.20.") || it.startsWith("192.168.49.")
        }
    }
}
