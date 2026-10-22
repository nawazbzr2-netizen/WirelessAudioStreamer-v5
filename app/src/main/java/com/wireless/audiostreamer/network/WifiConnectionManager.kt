package com.wireless.audiostreamer.network

import android.content.Context
import com.wireless.audiostreamer.util.AppPrefs
import com.wireless.audiostreamer.util.defaultWifiTarget
import com.wireless.audiostreamer.util.pickAutoConnectTarget
import com.wireless.audiostreamer.util.setDefaultWifiTarget

/**
 * Auto-connect for Nearby Wi-Fi peers (single peer or default target).
 */
class WifiConnectionManager(
    private val ctx: Context,
    private val onAutoConnect: (DiscoveryHelper.Peer) -> Unit
) {
    private var lastAutoConnectedIp: String? = null

    fun onPeersUpdated(peers: List<DiscoveryHelper.Peer>): List<DiscoveryHelper.Peer> {
        val target = pickAutoConnectTarget(
            candidates = peers,
            idOf = { it.ip },
            defaultId = AppPrefs.defaultWifiTarget(ctx)
        )
        if (target != null && target.ip != lastAutoConnectedIp) {
            lastAutoConnectedIp = target.ip
            onAutoConnect(target)
        }
        if (target == null) {
            lastAutoConnectedIp = null
        }
        return peers
    }

    fun setDefault(peer: DiscoveryHelper.Peer) {
        AppPrefs.setDefaultWifiTarget(ctx, peer.ip)
    }

    fun clearDefault() {
        AppPrefs.setDefaultWifiTarget(ctx, null)
    }
}
