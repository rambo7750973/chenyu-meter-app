package com.meter.app.ui.meter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.ble.BleManager
import com.meter.app.data.repository.MeterRepository
import com.meter.app.domain.model.Meter
import com.meter.app.domain.model.MeterReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MeterRepository,
    private val bleManager: BleManager
) : ViewModel() {
    
    private val meterId: String = savedStateHandle.get<String>("meterId") ?: ""
    
    private val _uiState = MutableStateFlow(MeterDetailUiState())
    val uiState: StateFlow<MeterDetailUiState> = _uiState.asStateFlow()
    
    val meter = repository.getMeterById(meterId)
    val latestReading = repository.getLatestReading(meterId)
    
    init {
        loadMeterData()
        observeBleData()
    }
    
    private fun loadMeterData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Load meter data from repository
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    private fun observeBleData() {
        viewModelScope.launch {
            bleManager.receivedData.collect { data ->
                if (data != null) {
                    // Parse the received data
                    // This is where you would implement the protocol parsing
                    // For now, we'll just log it
                    println("Received BLE data: ${data.contentToString()}")
                    
                    // Example parsing (customize based on actual protocol)
                    if (data.size >= 10) {
                        val currentPower = parsePower(data)
                        val voltage = parseVoltage(data)
                        val current = parseCurrent(data)
                        val totalEnergy = parseTotalEnergy(data)
                        
                        // Record the reading
                        repository.recordMeterReading(
                            meterId = meterId,
                            currentPower = currentPower,
                            voltage = voltage,
                            current = current,
                            totalEnergy = totalEnergy,
                            dailyEnergy = totalEnergy * 0.1f, // Example calculation
                            monthlyEnergy = totalEnergy * 0.3f // Example calculation
                        )
                        
                        // Update meter status
                        repository.updateMeterStatus(meterId, true)
                    }
                }
            }
        }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
                
                // Send command to request data
                // This depends on the actual protocol
                // Example: send a read command
                val command = byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x0A) // Example command
                bleManager.sendData(command)
                
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isRefreshing = false
                )
            }
        }
    }
    
    fun disconnect() {
        bleManager.disconnect()
        viewModelScope.launch {
            repository.updateMeterStatus(meterId, false)
        }
    }
    
    // Placeholder parsing functions - replace with actual protocol parsing
    private fun parsePower(data: ByteArray): Float {
        // Example: bytes 2-3 represent power in watts
        return if (data.size >= 4) {
            ((data[2].toInt() and 0xFF) shl 8 or (data[3].toInt() and 0xFF)).toFloat() / 10f
        } else {
            0f
        }
    }
    
    private fun parseVoltage(data: ByteArray): Float {
        // Example: bytes 4-5 represent voltage in volts
        return if (data.size >= 6) {
            ((data[4].toInt() and 0xFF) shl 8 or (data[5].toInt() and 0xFF)).toFloat() / 10f
        } else {
            0f
        }
    }
    
    private fun parseCurrent(data: ByteArray): Float {
        // Example: bytes 6-7 represent current in amps
        return if (data.size >= 8) {
            ((data[6].toInt() and 0xFF) shl 8 or (data[7].toInt() and 0xFF)).toFloat() / 100f
        } else {
            0f
        }
    }
    
    private fun parseTotalEnergy(data: ByteArray): Float {
        // Example: bytes 8-9 represent total energy in kWh
        return if (data.size >= 10) {
            ((data[8].toInt() and 0xFF) shl 8 or (data[9].toInt() and 0xFF)).toFloat() / 10f
        } else {
            0f
        }
    }
}

data class MeterDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)