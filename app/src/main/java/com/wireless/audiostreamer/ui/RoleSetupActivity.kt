package com.wireless.audiostreamer.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wireless.audiostreamer.R
import com.wireless.audiostreamer.service.StreamingService
import com.wireless.audiostreamer.util.AppPrefs

class RoleSetupActivity : AppCompatActivity() {

    private var chosenRole = AppPrefs.ROLE_RECEIVER
    private lateinit var etPin: EditText

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { afterRuntimePermissions() }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val i = Intent(this, StreamingService::class.java).apply {
                action = "projection_granted"
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
                putExtra("purpose", "screen")
            }
            try {
                startForegroundService(i)
            } catch (_: Exception) {
                startService(i)
            }
            Toast.makeText(this, "اسکرین / سسٹم ساؤنڈ تیار", Toast.LENGTH_SHORT).show()
        }
        requestBatteryOptimization()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppPrefs.isSetupDone(this)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_role_setup)

        val rbSender = findViewById<RadioButton>(R.id.rbSender)
        val rbReceiver = findViewById<RadioButton>(R.id.rbReceiver)
        val etName = findViewById<EditText>(R.id.etName)
        etPin = findViewById(R.id.etPin)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        etPin.setText("")
        etPin.hint = "PIN (4–8 نمبر)"

        etName.setText(AppPrefs.deviceName(this))
        if (AppPrefs.role(this) == AppPrefs.ROLE_SENDER) rbSender.isChecked = true
        else rbReceiver.isChecked = true

        setupPinPad()

        btnContinue.setOnClickListener { onContinue(rbSender, etName) }
        findViewById<Button>(R.id.pinOk).setOnClickListener { onContinue(rbSender, etName) }
    }

    private fun setupPinPad() {
        val append = { d: String ->
            val cur = etPin.text?.toString().orEmpty()
            if (cur.length < 8) etPin.setText(cur + d)
        }
        listOf(
            R.id.pin0 to "0", R.id.pin1 to "1", R.id.pin2 to "2", R.id.pin3 to "3",
            R.id.pin4 to "4", R.id.pin5 to "5", R.id.pin6 to "6",
            R.id.pin7 to "7", R.id.pin8 to "8", R.id.pin9 to "9"
        ).forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { append(digit) }
        }
        findViewById<Button>(R.id.pinDel).setOnClickListener {
            val cur = etPin.text?.toString().orEmpty()
            if (cur.isNotEmpty()) etPin.setText(cur.dropLast(1))
        }
    }

    private fun onContinue(rbSender: RadioButton, etName: EditText) {
        val pin = etPin.text?.toString()?.trim().orEmpty()
        if (pin.length < 4) {
            Toast.makeText(this, "PIN کم از کم 4 نمبر", Toast.LENGTH_SHORT).show()
            return
        }
        if (pin.length > 8) {
            Toast.makeText(this, "PIN زیادہ سے زیادہ 8 نمبر", Toast.LENGTH_SHORT).show()
            return
        }
        if (!pin.all { it.isDigit() }) {
            Toast.makeText(this, "صرف نمبر", Toast.LENGTH_SHORT).show()
            return
        }

        chosenRole = if (rbSender.isChecked) AppPrefs.ROLE_SENDER else AppPrefs.ROLE_RECEIVER
        val name = etName.text.toString().trim().ifEmpty { android.os.Build.MODEL }

        AppPrefs.setRole(this, chosenRole)
        AppPrefs.setPin(this, pin)
        AppPrefs.setDeviceName(this, name)
        AppPrefs.setSetupDone(this)
        AppPrefs.setSenderAlwaysOn(this, true)

        if (chosenRole == AppPrefs.ROLE_SENDER) explainSenderPermissions()
        else explainReceiverPermissions()
    }

    private fun explainSenderPermissions() {
        AlertDialog.Builder(this)
            .setTitle("Sender – مکمل کنٹرول")
            .setMessage(
                "Receiver آپ کے فون کو کنٹرول کرے گا۔\n\n" +
                    "1) Nearby / مائک / کیمرہ\n" +
                    "2) اسکرین کاسٹ (ابھی مانگی جائے گی)\n" +
                    "3) Accessibility (ریموٹ ٹیپ)\n" +
                    "4) بیک گراؤنڈ unrestricted"
            )
            .setPositiveButton("اجازت دیں") { _, _ -> requestAllRuntimePermissions() }
            .setCancelable(false)
            .show()
    }

    private fun explainReceiverPermissions() {
        AlertDialog.Builder(this)
            .setTitle("Receiver")
            .setMessage("Nearby / Location اور مائک اجازت دیں تاکہ فون ملے اور آواز آئے۔")
            .setPositiveButton("اجازت دیں") { _, _ -> requestAllRuntimePermissions() }
            .setCancelable(false)
            .show()
    }

    private fun requestAllRuntimePermissions() {
        val needed = mutableListOf<String>()
        fun need(p: String) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needed.add(p)
        }
        if (Build.VERSION.SDK_INT >= 33) need(Manifest.permission.NEARBY_WIFI_DEVICES)
        need(Manifest.permission.ACCESS_FINE_LOCATION)
        need(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 31) {
            need(Manifest.permission.BLUETOOTH_CONNECT)
            need(Manifest.permission.BLUETOOTH_SCAN)
        }
        need(Manifest.permission.RECORD_AUDIO)
        need(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) need(Manifest.permission.POST_NOTIFICATIONS)

        if (needed.isEmpty()) afterRuntimePermissions()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun afterRuntimePermissions() {
        if (chosenRole == AppPrefs.ROLE_SENDER) {
            requestScreenCastPermission()
        } else {
            goToMain()
        }
    }

    private fun requestScreenCastPermission() {
        AlertDialog.Builder(this)
            .setTitle("اسکرین کاسٹ")
            .setMessage("اسکرین شیئر اور سسٹم ساؤنڈ کے لیے Allow دبائیں۔")
            .setPositiveButton("اجازت") { _, _ ->
                try {
                    val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    @Suppress("DEPRECATION")
                    projectionLauncher.launch(mpm.createScreenCaptureIntent())
                } catch (e: Exception) {
                    Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
                    requestBatteryOptimization()
                }
            }
            .setNegativeButton("بعد میں") { _, _ -> requestBatteryOptimization() }
            .setCancelable(false)
            .show()
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            requestAccessibility(); return
        }
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            requestAccessibility(); return
        }
        AlertDialog.Builder(this)
            .setTitle("بیک گراؤنڈ")
            .setMessage("Unrestricted / No restrictions منتخب کریں۔")
            .setPositiveButton("کھولیں") { _, _ ->
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {}
                }
                window.decorView.postDelayed({ requestAccessibility() }, 800)
            }
            .setNegativeButton("بعد میں") { _, _ -> requestAccessibility() }
            .setCancelable(false)
            .show()
    }

    private fun requestAccessibility() {
        if (isAccessibilityServiceEnabled()) {
            goToMain()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Accessibility")
            .setMessage(getString(R.string.a11y_setup_msg))
            .setPositiveButton("Settings") { _, _ ->
                awaitingAccessibilityReturn = true
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {
                    goToMain()
                }
            }
            .setNegativeButton("بعد میں") { _, _ -> goToMain() }
            .setCancelable(false)
            .show()
    }

    private var awaitingAccessibilityReturn = false

    override fun onResume() {
        super.onResume()
        if (awaitingAccessibilityReturn) {
            awaitingAccessibilityReturn = false
            if (isAccessibilityServiceEnabled()) {
                goToMain()
            } else {
                requestAccessibility()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val expected = "$packageName/${com.wireless.audiostreamer.service.RemoteControlAccessibilityService::class.java.name}"
            val enabled = Settings.Secure.getString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
