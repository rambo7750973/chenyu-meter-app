package com.meter.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BleManager"

        val CUSTOM_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val CUSTOM_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    val receivedData: StateFlow<ByteArray?> = _receivedData.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            val rssi = result.rssi

            if (deviceName.contains("Chenyu", ignoreCase = true) ||
                deviceName.contains("Meter", ignoreCase = true) ||
                deviceName.contains("电表", ignoreCase = true) ||
                deviceName.contains("HLW", ignoreCase = true) ||
                deviceName.contains("BL09", ignoreCase = true) ||
                deviceName.contains("CW", ignoreCase = true)
            ) {
                val bleDevice = BleDevice(
                    name = deviceName,
                    address = deviceAddress,
                    rssi = rssi,
                    device = device
                )

                val currentDevices = _discoveredDevices.value.toMutableList()
                if (currentDevices.none { it.address == deviceAddress }) {
                    currentDevices.add(bleDevice)
                    _discoveredDevices.value = currentDevices
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
            _scanState.value = ScanState.Error("扫描失败: $errorCode")
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions()) {
            _scanState.value = ScanState.Error("缺少蓝牙权限")
            return
        }
        if (!isBluetoothEnabled()) {
            _scanState.value = ScanState.Error("蓝牙未开启")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _scanState.value = ScanState.Error("BLE不可用")
            return
        }

        _scanState.value = ScanState.Scanning
        _discoveredDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)

        handler.postDelayed({ stopScan() }, 15000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_scanState.value is ScanState.Scanning) {
            _scanState.value = ScanState.Stopped
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BleDevice) {
        if (!hasPermissions()) {
            _connectionState.value = ConnectionState.Error("缺少蓝牙权限")
            return
        }

        stopScan()
        _connectionState.value = ConnectionState.Connecting

        bluetoothGatt = device.device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Connected to ${device.name}")
                        _connectionState.value = ConnectionState.Connected(device.name, device.address)
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Disconnected")
                        _connectionState.value = ConnectionState.Disconnected
                        gatt.close()
                        bluetoothGatt = null
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered")
                    val service = gatt.getService(CUSTOM_SERVICE_UUID)
                    if (service != null) {
                        val characteristic = service.getCharacteristic(CUSTOM_CHARACTERISTIC_UUID)
                        if (characteristic != null) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR)
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value
                if (data != null) {
                    Log.d(TAG, "Received: ${data.joinToString(" ") { "%02X".format(it) }}")
                    _receivedData.value = data
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    fun sendData(data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(CUSTOM_SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CUSTOM_CHARACTERISTIC_UUID) ?: return
        characteristic.value = data
        gatt.writeCharacteristic(characteristic)
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice
)

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    object Stopped : ScanState()
    data class Error(val message: String) : ScanState()
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}