package com.meter.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.ble.BleDevice
import com.meter.app.ble.BleManager
import com.meter.app.ble.ScanState
import com.meter.app.data.repository.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository: MeterRepository,
    private val bleManager: BleManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val meters = repository.getAllMeters()
    val scanState = bleManager.scanState
    val discoveredDevices = bleManager.discoveredDevices

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val today = repository.getTodayEnergy("")
                val month = repository.getMonthEnergy("")
                _uiState.value = _uiState.value.copy(
                    todayEnergy = today,
                    monthEnergy = month,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(device: BleDevice) {
        viewModelScope.launch {
            // Create meter entry
            repository.createMeter(
                name = device.name,
                address = device.address,
                macAddress = device.address
            )
            // Connect via BLE
            bleManager.connectToDevice(device)
        }
    }

    fun refreshStats() {
        loadStats()
    }
}

data class HomeUiState(
    val todayEnergy: Float = 0f,
    val monthEnergy: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)