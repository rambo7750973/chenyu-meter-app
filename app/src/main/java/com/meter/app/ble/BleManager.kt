package com.meter.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
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
        
        // Common BLE Service UUIDs
        val CUSTOM_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val CUSTOM_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()
    
    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    val receivedData: StateFlow<ByteArray?> = _receivedData.asStateFlow()
    
    private val scanCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        object : android.bluetooth.le.ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
                val device = result.device
                val deviceName = device.name ?: "Unknown"
                val deviceAddress = device.address
                val rssi = result.rssi
                
                // Filter for Chenyu meters (customize this filter)
                if (deviceName.contains("Chenyu", ignoreCase = true) || 
                    deviceName.contains("Meter", ignoreCase = true) ||
                    deviceName.contains("电表", ignoreCase = true)) {
                    
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
                Log.e(TAG, "Scan failed with error code: $errorCode")
                _scanState.value = ScanState.Error("Scan failed with error code: $errorCode")
            }
        }
    } else {
        TODO("VERSION.SDK_INT < LOLLIPOP")
    }
    
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasBluetoothPermissions()) {
            _scanState.value = ScanState.Error("Bluetooth permissions not granted")
            return
        }
        
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            _scanState.value = ScanState.Error("Bluetooth not available")
            return
        }
        
        _scanState.value = ScanState.Scanning
        _discoveredDevices.value = emptyList()
        
        // Start scan with filter for Chenyu meters
        val filters = listOf(
            android.bluetooth.le.ScanFilter.Builder()
                .setDeviceName("Chenyu") // Customize this
                .build()
        )
        
        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothLeScanner.startScan(filters, settings, scanCallback)
        
        // Stop scan after 10 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopScan()
        }, 10000)
    }
    
    @SuppressLint("MissingPermission")
    fun stopScan() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        bluetoothLeScanner?.stopScan(scanCallback)
        _scanState.value = ScanState.Stopped
    }
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BleDevice) {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = ConnectionState.Error("Bluetooth permissions not granted")
            return
        }
        
        _connectionState.value = ConnectionState.Connecting
        
        bluetoothGatt = device.device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Connected to GATT server")
                        _connectionState.value = ConnectionState.Connected(device.name, device.address)
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Disconnected from GATT server")
                        _connectionState.value = ConnectionState.Disconnected
                        gatt.close()
                    }
                }
            }
            
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered")
                    // Try to find and subscribe to the custom service
                    val service = gatt.getService(CUSTOM_SERVICE_UUID)
                    if (service != null) {
                        val characteristic = service.getCharacteristic(CUSTOM_CHARACTERISTIC_UUID)
                        if (characteristic != null) {
                            // Enable notifications
                            gatt.setCharacteristicNotification(characteristic, true)
                            val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR)
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                } else {
                    Log.e(TAG, "onServicesDiscovered failed with status: $status")
                }
            }
            
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value
                Log.d(TAG, "Received data: ${data?.toHex()}")
                _receivedData.value = data
            }
            
            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Characteristic write successful")
                } else {
                    Log.e(TAG, "Characteristic write failed with status: $status")
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
    
    private fun hasBluetoothPermissions(): Boolean {
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

// Extension function to convert ByteArray to hex string
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }