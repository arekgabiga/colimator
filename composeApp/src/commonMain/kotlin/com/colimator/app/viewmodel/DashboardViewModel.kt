package com.colimator.app.viewmodel

import com.colimator.app.service.ColimaService
import com.colimator.app.service.VmStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Dashboard screen.
 */
data class DashboardState(
    val vmStatus: VmStatus = VmStatus.Unknown,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Colima VM management.
 */
class DashboardViewModel(private val colimaService: ColimaService) : BaseViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val status = colimaService.getStatus()
                _state.update { it.copy(vmStatus = status, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to get status", isLoading = false) }
            }
        }
    }

    fun startVm() {
        viewModelScope.launch {
            _state.update { it.copy(vmStatus = VmStatus.Starting, isLoading = true, error = null) }
            val result = colimaService.start()
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr.ifBlank { "Failed to start Colima" }, isLoading = false) }
            }
            refreshStatus()
        }
    }

    fun stopVm() {
        viewModelScope.launch {
            _state.update { it.copy(vmStatus = VmStatus.Stopping, isLoading = true, error = null) }
            val result = colimaService.stop()
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr.ifBlank { "Failed to stop Colima" }, isLoading = false) }
            }
            refreshStatus()
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
