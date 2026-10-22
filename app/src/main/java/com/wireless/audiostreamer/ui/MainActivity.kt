package com.wireless.audiostreamer.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothManager
import com.wireless.audiostreamer.R
import com.wireless.audiostreamer.network.BtSignaling
import com.wireless.audiostreamer.network.DiscoveryHelper
import com.wireless.audiostreamer.network.InternetSignaling
import com.wireless.audiostreamer.network.NetUtil
import com.wireless.audiostreamer.network.PacketCipher
import com.wireless.audiostreamer.network.WifiConnectionManager
import com.wireless.audiostreamer.service.StreamingService
import com.wireless.audiostreamer.util.AppPrefs

class MainActivity : AppCompatActivity() {

    private enum class Feature { NONE, MIC, BACK_CAM, FRONT_CAM, SYS_SOUND, SYS_MIC, SCREEN }

    private lateinit var discovery: DiscoveryHelper
    private var internetSig: InternetSignaling? = null
    private var btSig: BtSignaling? = null
    private var wifiMgr: WifiConnectionManager? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvRole: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvMyIp: TextView
    private lateinit var tvPeerList: TextView
    private lateinit var tvSelectedPeer: TextView
    private lateinit var btnScanDevices: Button
    private lateinit var btnMicOpen: Button
    private lateinit var btnMicStop: Button
    private lateinit var btnMicRecord: Button
    private lateinit var btnMicStopRecord: Button
    private lateinit var btnBackCamOpen: Button
    private lateinit var btnBackCamStop: Button
    private lateinit var btnBackCamRecord: Button
    private lateinit var btnFrontCamOpen: Button
    private lateinit var btnFrontCamStop: Button
    private lateinit var btnFrontCamRecord: Button
    private lateinit var btnSysSoundOpen: Button
    private lateinit var btnSysSoundStop: Button
    private lateinit var btnSysMicOpen: Button
    private lateinit var btnSysMicStop: Button
    private lateinit var btnVoiceRecord: Button
    private lateinit var btnVoiceStop: Button
    private lateinit var btnScreenOpen: Button
    private lateinit var btnScreenStop: Button
    private lateinit var btnSettings: Button
    private lateinit var btnSwitchRole: Button
    private lateinit var etRemoteIp: EditText
    private lateinit var etManualIp: EditText

    private val localPeers = ArrayList<DiscoveryHelper.Peer>()
    private val btPeerIps = HashSet<String>()
    private var selectedPeerIp: String? = null
    private var selectedPeerName: String = ""
    private var connectionType: String = ""
    private var liveConnected = false
    private var activeFeature = Feature.NONE
    private var localScanStartedAt = 0L

    private val autoScan = object : Runnable {
        override fun run() {
            try { discovery.scanNow() } catch (_: Exception) {}
            handler.postDelayed(this, 4000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        updateRoleUi()
        showLocalIp()
        liveConnected = false
        tvStatus.text = "Nearby تلاش…"
        localScanStartedAt = System.currentTimeMillis()
        val cipher = PacketCipher(AppPrefs.pin(this))
        discovery = DiscoveryHelper(this, cipher)
        discovery.onPeers = { peers -> runOnUiThread { onLocalPeers(peers) } }
        discovery.start(AppPrefs.deviceName(this), AppPrefs.role(this), AppPrefs.manualIp(this))
        wifiMgr = WifiConnectionManager(this) { peer ->
            selectLivePeer(peer.ip, peer.name, detectLocalType(peer.ip))
        }
        startBluetoothSignaling()
        if (AppPrefs.role(this) == AppPrefs.ROLE_SENDER) {
            startServiceAction("sender_listen")
            AppPrefs.setSenderAlwaysOn(this, true)
        }
        setupClickListeners()
        discovery.scanNow()
        handler.postDelayed(autoScan, 1200)
        ensureInternetSignaling()
    }

    // --- remaining methods continue in full source; this truncated version will be replaced ---
    private fun onLocalPeers(peers: List<DiscoveryHelper.Peer>) {}
    private fun peerPriority(ip: String): Int = 1
    private fun autoConnectLocal(peers: List<DiscoveryHelper.Peer>) {}
    private fun detectLocalType(ip: String): String = "Same Wi-Fi"
    private fun startBluetoothSignaling() {}
    private fun selectLivePeer(ip: String, name: String, type: String) {}
    private fun rebuildPeerList() {}
    private fun ensureInternetSignaling() {}
    private fun startLocationUpdatesIfPermitted() {}
    private fun stopLocationUpdates() {}
    private fun refreshConnectionLabel() {}
    private fun bindViews() {
        tvRole = findViewById(R.id.tvRole)
        tvStatus = findViewById(R.id.tvStatus)
        tvMyIp = findViewById(R.id.tvMyIp)
        tvPeerList = findViewById(R.id.tvPeerList)
        tvSelectedPeer = findViewById(R.id.tvSelectedPeer)
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnMicOpen = findViewById(R.id.btnMicOpen)
        btnMicStop = findViewById(R.id.btnMicStop)
        btnMicRecord = findViewById(R.id.btnMicRecord)
        btnMicStopRecord = findViewById(R.id.btnMicStopRecord)
        btnBackCamOpen = findViewById(R.id.btnBackCamOpen)
        btnBackCamStop = findViewById(R.id.btnBackCamStop)
        btnBackCamRecord = findViewById(R.id.btnBackCamRecord)
        btnFrontCamOpen = findViewById(R.id.btnFrontCamOpen)
        btnFrontCamStop = findViewById(R.id.btnFrontCamStop)
        btnFrontCamRecord = findViewById(R.id.btnFrontCamRecord)
        btnSysSoundOpen = findViewById(R.id.btnSysSoundOpen)
        btnSysSoundStop = findViewById(R.id.btnSysSoundStop)
        btnSysMicOpen = findViewById(R.id.btnSysMicOpen)
        btnSysMicStop = findViewById(R.id.btnSysMicStop)
        btnVoiceRecord = findViewById(R.id.btnVoiceRecord)
        btnVoiceStop = findViewById(R.id.btnVoiceStop)
        btnScreenOpen = findViewById(R.id.btnScreenOpen)
        btnScreenStop = findViewById(R.id.btnScreenStop)
        btnSettings = findViewById(R.id.btnSettings)
        btnSwitchRole = findViewById(R.id.btnSwitchRole)
        etRemoteIp = findViewById(R.id.etRemoteIp)
        etManualIp = findViewById(R.id.etManualIp)
    }
    private fun updateRoleUi() { tvRole.text = AppPrefs.role(this) }
    private fun showLocalIp() { tvMyIp.text = NetUtil.localIps().joinToString() }
    private fun setupClickListeners() {}
    private fun startServiceAction(action: String) {
        val i = Intent(this, StreamingService::class.java).apply { this.action = action }
        try { startForegroundService(i) } catch (_: Exception) { startService(i) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoScan)
        internetSig?.stop()
        stopLocationUpdates()
        btSig?.stop()
        discovery.stop()
        super.onDestroy()
    }
}
