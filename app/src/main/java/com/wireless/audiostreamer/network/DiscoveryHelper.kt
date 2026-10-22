package com.wireless.audiostreamer.network

import android.content.Context
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * UDP discovery on same Wi-Fi / Hotspot. Faster announce + immediate scan burst.
 */
class DiscoveryHelper(private val ctx: Context, private val cipher: PacketCipher) {
    data class Peer(val ip: String, val name: String, val role: String, val seen: Long)

    @Volatile var onPeers: ((List<Peer>) -> Unit)? = null

    private val peers = ConcurrentHashMap<String, Peer>()
    private val unicast = ConcurrentHashMap<String, Long>()
    private val session = SecureRandom().nextLong()
    private var seq = 0
    @Volatile private var running = false
    @Volatile private var rxSock: DatagramSocket? = null
    private val threads = ArrayList<Thread>()
    @Volatile private var manualIp = ""
    private var name = ""
    private var role = ""
    private val burstAnnounce = AtomicBoolean(false)

    fun start(name: String, role: String, manualIp: String) {
        if (running) return
        this.name = name
        this.role = role
        this.manualIp = manualIp
        running = true
        threads.add(thread(isDaemon = true, name = "was-disc-rx") { rxLoop() })
        threads.add(thread(isDaemon = true, name = "was-disc-tx") { txLoop() })
    }

    fun stop() {
        running = false
        try { rxSock?.close() } catch (_: Exception) {}
        for (t in threads) t.interrupt()
        threads.clear()
        peers.clear()
    }

    fun scanNow() {
        if (!running) return
        burstAnnounce.set(true)
        publish()
        thread(isDaemon = true) { sendAnnounceBurst() }
    }

    fun setManualIp(ip: String) {
        manualIp = ip
        scanNow()
    }

    fun lastSeen(ip: String): Long = peers[ip]?.seen ?: 0L
    fun peer(ip: String): Peer? = peers[ip]
    fun allPeers(): List<Peer> = peers.values.sortedBy { it.name }

    private fun rxLoop() {
        val buf = ByteArray(1024)
        while (running) {
            var s: DatagramSocket? = null
            try {
                s = DatagramSocket(null as SocketAddress?)
                s.reuseAddress = true
                s.broadcast = true
                s.bind(InetSocketAddress(Protocol.PORT_DISCOVERY))
                NetUtil.bindToWifi(ctx, s)
                rxSock = s
                while (running) {
                    val pkt = DatagramPacket(buf, buf.size)
                    s.receive(pkt)
                    handle(pkt)
                }
            } catch (e: Exception) {
                if (running) {
                    try { Thread.sleep(800) } catch (_: InterruptedException) { return }
                }
            } finally {
                try { s?.close() } catch (_: Exception) {}
                rxSock = null
            }
        }
    }

    private fun handle(pkt: DatagramPacket) {
        val ip = pkt.address?.hostAddress?.substringBefore("%") ?: return
        if (!NetUtil.isPrivateIp(ip) || NetUtil.localIps().contains(ip)) return
        val o = cipher.open(pkt.data, pkt.length) ?: return
        if (o.type != Protocol.T_HELLO) return
        val h = Protocol.parseHello(o.plain) ?: return
        val now = System.currentTimeMillis()
        peers[ip] = Peer(ip, h.first, h.second, now)
        unicast[ip] = now
        publish()
    }

    private fun sendAnnounceBurst() {
        repeat(3) {
            try {
                announceOnce()
                Thread.sleep(200)
            } catch (_: Exception) {}
        }
    }

    private fun announceOnce() {
        val frame = cipher.seal(Protocol.T_HELLO, session, seq++, Protocol.helloPlain(name, role))
        val targets = LinkedHashSet<InetAddress>()
        targets.addAll(NetUtil.broadcastAddresses())
        val manual = manualIp
        if (NetUtil.isPrivateIp(manual)) {
            try { targets.add(InetAddress.getByName(manual)) } catch (_: Exception) {}
        }
        val now = System.currentTimeMillis()
        for ((ip, t) in unicast) {
            if (now - t < 20_000) {
                if (NetUtil.isPrivateIp(ip)) {
                    try { targets.add(InetAddress.getByName(ip)) } catch (_: Exception) {}
                }
            }
        }
        for (t in targets) {
            try {
                val sock = DatagramSocket()
                NetUtil.bindToWifi(ctx, sock)
                sock.broadcast = true
                sock.send(DatagramPacket(frame, frame.size, t, Protocol.PORT_DISCOVERY))
                sock.close()
            } catch (_: Exception) {}
        }
    }

    private fun txLoop() {
        while (running) {
            try {
                if (burstAnnounce.compareAndSet(true, false)) {
                    sendAnnounceBurst()
                } else {
                    announceOnce()
                }
                val now = System.currentTimeMillis()
                var changed = false
                for ((ip, p) in peers) {
                    if (now - p.seen > 12_000) {
                        peers.remove(ip)
                        changed = true
                    }
                }
                if (changed) publish()
                Thread.sleep(1500)
            } catch (e: InterruptedException) {
                return
            } catch (e: Exception) {
                try { Thread.sleep(800) } catch (_: InterruptedException) { return }
            }
        }
    }

    private fun publish() {
        onPeers?.invoke(peers.values.sortedBy { it.name })
    }
}
