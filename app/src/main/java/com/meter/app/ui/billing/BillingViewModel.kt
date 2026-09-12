package com.meter.app.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meter.app.data.repository.MeterRepository
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

    val billingRecords = repository.getBillingRecords("")

    init {
        loadBillingData()
    }

    private fun loadBillingData() {
        viewModelScope.launch {
            try {
                val unpaid = repository.getUnpaidAmount("")
                val paid = repository.getPaidAmount("")
                _uiState.value = _uiState.value.copy(
                    unpaidAmount = unpaid,
                    paidAmount = paid,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

data class BillingUiState(
    val unpaidAmount: Float = 0f,
    val paidAmount: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)