package com.wireless.audiostreamer.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wireless.audiostreamer.R
import com.wireless.audiostreamer.audio.AdpcmCodec
import com.wireless.audiostreamer.audio.JitterBuffer
import com.wireless.audiostreamer.audio.SystemSoundCapturer
import com.wireless.audiostreamer.camera.CameraCapturer
import com.wireless.audiostreamer.camera.VideoEncoder
import com.wireless.audiostreamer.network.BtSignaling
import com.wireless.audiostreamer.network.DiscoveryHelper
import com.wireless.audiostreamer.network.InternetSignaling
import com.wireless.audiostreamer.network.NetUtil
import com.wireless.audiostreamer.network.PacketCipher
import com.wireless.audiostreamer.network.Protocol
import com.wireless.audiostreamer.network.WifiConnectionManager
import com.wireless.audiostreamer.screen.ScreenCapturer
import com.wireless.audiostreamer.ui.MainActivity
import com.wireless.audiostreamer.util.AppPrefs
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * PLACEHOLDER - full StreamingService will be restored from source zip in next commit.
 * Minimal stub so BootReceiver compiles; full implementation follows.
 */
class StreamingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    companion object {
        const val ACTION_STOP_RECORDING = "com.wireless.audiostreamer.STOP_RECORDING"
    }
}
