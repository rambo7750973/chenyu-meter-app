package com.meter.app.ble

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiConfigManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "WiFiConfigManager"
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _availableNetworks = MutableStateFlow<List<WiFiNetwork>>(emptyList())
    val availableNetworks: StateFlow<List<WiFiNetwork>> = _availableNetworks.asStateFlow()

    private val _configState = MutableStateFlow<WiFiConfigState>(WiFiConfigState.Idle)
    val configState: StateFlow<WiFiConfigState> = _configState.asStateFlow()

    private val _currentWiFiStatus = MutableStateFlow<WiFiStatus>(WiFiStatus.Unknown)
    val currentWiFiStatus: StateFlow<WiFiStatus> = _currentWiFiStatus.asStateFlow()

    @SuppressLint("MissingPermission")
    fun scanNetworks(): List<WiFiNetwork> {
        _configState.value = WiFiConfigState.Scanning

        val results = wifiManager.scanResults ?: emptyList()
        val networks = results.map { result ->
            WiFiNetwork(
                ssid = result.SSID.removePrefix("\"").removeSuffix("\""),
                bssid = result.BSSID,
                signalStrength = calculateSignalLevel(result.level),
                security = getSecurityType(result.capabilities),
                capabilities = result.capabilities
            )
        }.filter { it.ssid.isNotEmpty() }
            .distinctBy { it.ssid }
            .sortedByDescending { it.signalStrength }

        _availableNetworks.value = networks
        _configState.value = WiFiConfigState.ScanComplete(networks)
        return networks
    }

    fun getCurrentNetwork(): WiFiNetwork? {
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo == null || wifiInfo.networkId == -1) return null

        return WiFiNetwork(
            ssid = wifiInfo.ssid.removePrefix("\"").removeSuffix("\""),
            bssid = wifiInfo.bssid,
            signalStrength = calculateSignalLevel(wifiInfo.rssi),
            security = "",
            capabilities = ""
        )
    }

    fun isWiFiEnabled(): Boolean = wifiManager.isWifiEnabled

    @SuppressLint("MissingPermission")
    fun buildWiFiConfigCommand(ssid: String, password: String, bleManager: BleManager): Boolean {
        _configState.value = WiFiConfigState.Configuring

        return try {
            // Build WiFi configuration command for the meter
            // Protocol: 0xAA + CMD(0x01) + SSID_LEN + SSID + PASS_LEN + PASS + CHECKSUM
            val ssidBytes = ssid.toByteArray(Charsets.UTF_8)
            val passBytes = password.toByteArray(Charsets.UTF_8)

            val command = mutableListOf<Byte>()
            command.add(0xAA.toByte()) // Header
            command.add(0x01) // WiFi config command
            command.add(ssidBytes.size.toByte())
            command.addAll(ssidBytes.toList())
            command.add(passBytes.size.toByte())
            command.addAll(passBytes.toList())

            // Calculate checksum
            val checksum = command.sumOf { it.toInt() and 0xFF } and 0xFF
            command.add(checksum.toByte())

            val commandArray = command.toByteArray()
            Log.d(TAG, "Sending WiFi config: ${commandArray.joinToString(" ") { "%02X".format(it) }}")

            bleManager.sendData(commandArray)
            _configState.value = WiFiConfigState.ConfigSent
            true
        } catch (e: Exception) {
            Log.e(TAG, "WiFi config failed", e)
            _configState.value = WiFiConfigState.Error("发送配置失败: ${e.message}")
            false
        }
    }

    fun resetState() {
        _configState.value = WiFiConfigState.Idle
    }

    private fun calculateSignalLevel(rssi: Int): Int {
        return WifiManager.calculateSignalLevel(rssi, 5)
    }

    private fun getSecurityType(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "OPEN"
        }
    }
}

data class WiFiNetwork(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val security: String,
    val capabilities: String
)

sealed class WiFiConfigState {
    object Idle : WiFiConfigState()
    object Scanning : WiFiConfigState()
    data class ScanComplete(val networks: List<WiFiNetwork>) : WiFiConfigState()
    object Configuring : WiFiConfigState()
    object ConfigSent : WiFiConfigState()
    data class Success(val message: String = "WiFi配置已发送") : WiFiConfigState()
    data class Error(val message: String) : WiFiConfigState()
}

sealed class WiFiStatus {
    object Unknown : WiFiStatus()
    object Disconnected : WiFiStatus()
    data class Connected(val ssid: String, val signalStrength: Int) : WiFiStatus()
    object Configuring : WiFiStatus()
}