package com.meter.app.ui.meter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.ble.BleManager
import com.meter.app.ble.ConnectionState
import com.meter.app.ble.ScanState
import com.meter.app.data.repository.MeterRepository
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

    val connectionState = bleManager.connectionState
    val receivedData = bleManager.receivedData

    init {
        observeBleData()
    }

    private fun observeBleData() {
        viewModelScope.launch {
            bleManager.receivedData.collect { data ->
                if (data != null && data.size >= 10) {
                    val reading = parseMeterData(data)
                    if (reading != null) {
                        repository.recordMeterReading(
                            meterId = meterId,
                            currentPower = reading.first,
                            voltage = reading.second,
                            current = reading.third,
                            totalEnergy = reading.fourth,
                            dailyEnergy = reading.fourth * 0.05f,
                            monthlyEnergy = reading.fourth * 0.3f
                        )
                        repository.updateMeterStatus(meterId, true)
                    }
                }
            }
        }

        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Disconnected -> {
                        repository.updateMeterStatus(meterId, false)
                    }
                    is ConnectionState.Connected -> {
                        repository.updateMeterStatus(meterId, true)
                        requestMeterData()
                    }
                    else -> {}
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            requestMeterData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private fun requestMeterData() {
        // Common read command for many Chinese meters (HLW/BL09/CW chips)
        // Adjust based on actual protocol
        val command = byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x0A, (0xC5.toByte()), 0xCD.toByte())
        bleManager.sendData(command)
    }

    fun disconnect() {
        bleManager.disconnect()
        viewModelScope.launch {
            repository.updateMeterStatus(meterId, false)
        }
    }

    private fun parseMeterData(data: ByteArray): MeterReadingData? {
        return try {
            if (data.size < 10) return null

            val voltage = ((data[2].toInt() and 0xFF) shl 8 or (data[3].toInt() and 0xFF)).toFloat() / 10f
            val current = ((data[4].toInt() and 0xFF) shl 8 or (data[5].toInt() and 0xFF)).toFloat() / 1000f
            val power = ((data[6].toInt() and 0xFF) shl 8 or (data[7].toInt() and 0xFF)).toFloat() / 10f
            val energy = ((data[8].toInt() and 0xFF) shl 8 or (data[9].toInt() and 0xFF)).toFloat() / 100f

            MeterReadingData(power, voltage, current, energy)
        } catch (e: Exception) {
            null
        }
    }

    data class MeterReadingData(
        val power: Float,
        val voltage: Float,
        val current: Float,
        val energy: Float
    )
}

data class MeterDetailUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)