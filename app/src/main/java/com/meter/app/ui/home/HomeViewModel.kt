package com.meter.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.data.repository.MeterRepository
import com.meter.app.domain.model.Meter
import com.meter.app.domain.model.MeterStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MeterRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val meters = repository.getAllMeters()
    
    private var isScanning = false
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            try {
                // Load today's energy
                val todayEnergy = repository.getMeterStats("").todayEnergy
                val monthEnergy = repository.getMeterStats("").monthEnergy
                
                _uiState.value = _uiState.value.copy(
                    todayEnergy = todayEnergy,
                    monthEnergy = monthEnergy,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun startScan() {
        if (isScanning) return
        
        isScanning = true
        _uiState.value = _uiState.value.copy(isScanning = true)
        
        // TODO: Implement BLE scanning
        // For now, just simulate scanning
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            isScanning = false
            _uiState.value = _uiState.value.copy(isScanning = false)
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
    val isScanning: Boolean = false,
    val error: String? = null
)