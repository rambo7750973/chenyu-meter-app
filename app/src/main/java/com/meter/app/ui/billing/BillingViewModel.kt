package com.meter.app.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.data.repository.MeterRepository
import com.meter.app.domain.model.BillingRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: MeterRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()
    
    val billingRecords = repository.getBillingRecords("") // TODO: Get meterId
    
    init {
        loadBillingData()
    }
    
    private fun loadBillingData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val unpaidAmount = repository.getUnpaidAmount("")
                val paidAmount = repository.getPaidAmount("")
                
                _uiState.value = _uiState.value.copy(
                    unpaidAmount = unpaidAmount,
                    paidAmount = paidAmount,
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
    
    fun refreshData() {
        loadBillingData()
    }
}

data class BillingUiState(
    val unpaidAmount: Float = 0f,
    val paidAmount: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)