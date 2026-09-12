package com.meter.app.ui.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.ble.BleManager
import com.meter.app.ble.WiFiConfigManager
import com.meter.app.ble.WiFiConfigState
import com.meter.app.ble.WiFiNetwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WiFiConfigViewModel @Inject constructor(
    private val wifiConfigManager: WiFiConfigManager,
    private val bleManager: BleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WiFiConfigUiState())
    val uiState: StateFlow<WiFiConfigUiState> = _uiState.asStateFlow()

    val configState = wifiConfigManager.configState
    val networks = wifiConfigManager.availableNetworks

    init {
        observeConfigState()
    }

    private fun observeConfigState() {
        viewModelScope.launch {
            wifiConfigManager.configState.collect { state ->
                when (state) {
                    is WiFiConfigState.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = "WiFi配置已发送成功！设备将自动重启并连接WiFi",
                            isError = false
                        )
                    }
                    is WiFiConfigState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            message = state.message,
                            isError = true
                        )
                    }
                    is WiFiConfigState.ConfigSent -> {
                        _uiState.value = _uiState.value.copy(
                            message = "配置已发送，等待设备连接WiFi...",
                            isError = false
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun scanNetworks() {
        wifiConfigManager.scanNetworks()
    }

    fun sendWiFiConfig(ssid: String, password: String) {
        viewModelScope.launch {
            val success = wifiConfigManager.buildWiFiConfigCommand(ssid, password, bleManager)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    message = "正在发送WiFi配置...",
                    isError = false
                )
            }
        }
    }

    fun resetState() {
        wifiConfigManager.resetState()
        _uiState.value = WiFiConfigUiState()
    }
}

data class WiFiConfigUiState(
    val message: String = "",
    val isError: Boolean = false
)