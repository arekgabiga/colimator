package com.colimator.app.viewmodel

import com.colimator.app.service.ColimaService
import com.colimator.app.service.VmStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(private val colimaService: ColimaService) : BaseViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        // Initial check
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val status = colimaService.getStatus()
            _state.update { it.copy(vmStatus = status) }
        }
    }

    fun startVm() {
        viewModelScope.launch {
            _state.update { it.copy(vmStatus = VmStatus.Starting) }
            val result = colimaService.start()
            // In a real app we'd check result.isSuccess() and show errors
            refreshStatus()
        }
    }

    fun stopVm() {
        viewModelScope.launch {
            _state.update { it.copy(vmStatus = VmStatus.Stopping) }
            colimaService.stop()
            refreshStatus()
        }
    }
}

data class DashboardState(
    val vmStatus: VmStatus = VmStatus.Unknown
)
