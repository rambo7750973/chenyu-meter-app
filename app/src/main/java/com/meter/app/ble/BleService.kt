package com.meter.app.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meter.app.R
import com.meter.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BleService : Service() {
    
    companion object {
        private const val TAG = "BleService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ble_service_channel"
    }
    
    @Inject
    lateinit var bleManager: BleManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isScanning = false
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "BleService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_START_SCAN" -> startScanning()
            "ACTION_STOP_SCAN" -> stopScanning()
            "ACTION_CONNECT" -> {
                val deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""
                val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS") ?: ""
                connectToDevice(deviceName, deviceAddress)
            }
            "ACTION_DISCONNECT" -> disconnect()
            "ACTION_SEND_DATA" -> {
                val data = intent.getByteArrayExtra("DATA")
                if (data != null) {
                    sendData(data)
                }
            }
        }
        
        return START_STICKY
    }
    
    private fun startScanning() {
        if (isScanning) return
        
        serviceScope.launch {
            bleManager.startScan()
            isScanning = true
            startForeground(NOTIFICATION_ID, createNotification("Scanning for devices..."))
        }
    }
    
    private fun stopScanning() {
        bleManager.stopScan()
        isScanning = false
        updateNotification("Scan stopped")
    }
    
    private fun connectToDevice(deviceName: String, deviceAddress: String) {
        serviceScope.launch {
            val device = bleManager.discoveredDevices.value.find { 
                it.name == deviceName && it.address == deviceAddress 
            }
            if (device != null) {
                bleManager.connectToDevice(device)
                updateNotification("Connected to $deviceName")
            }
        }
    }
    
    private fun disconnect() {
        bleManager.disconnect()
        updateNotification("Disconnected")
    }
    
    private fun sendData(data: ByteArray) {
        serviceScope.launch {
            bleManager.sendData(data)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BLE connection service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meter App")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        bleManager.disconnect()
        Log.d(TAG, "BleService destroyed")
    }
}