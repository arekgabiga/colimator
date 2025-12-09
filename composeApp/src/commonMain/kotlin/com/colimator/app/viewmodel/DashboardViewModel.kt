package com.colimator.app.viewmodel

import com.colimator.app.service.ActiveProfileRepository
import com.colimator.app.service.ColimaConfig
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
    val vmConfig: ColimaConfig? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeProfile: String = "default",
    val showDeleteConfirmation: Boolean = false
)

/**
 * ViewModel for Colima VM management.
 */
class DashboardViewModel(
    private val colimaService: ColimaService,
    private val activeProfileRepository: ActiveProfileRepository
) : BaseViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        // Subscribe to profile changes
        viewModelScope.launch {
            activeProfileRepository.activeProfile.collect { profile ->
                val profileName = profile ?: "default"
                _state.update { it.copy(activeProfile = profileName) }
                refreshStatus()
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val profileName = activeProfileRepository.activeProfile.value
                val status = colimaService.getStatus(profileName)
                val config = if (status == VmStatus.Running) colimaService.getConfig(profileName) else null
                _state.update { it.copy(vmStatus = status, vmConfig = config, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(error = e.message ?: "Failed to get status", isLoading = false) }
            }
        }
    }

    fun startVm() {
        viewModelScope.launch {
            _state.update { it.copy(vmStatus = VmStatus.Starting, isLoading = true, error = null) }
            val profileName = activeProfileRepository.activeProfile.value
            val result = colimaService.start(profileName)
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr.ifBlank { "Failed to start Colima" }, isLoading = false) }
            }
            refreshStatus()
        }
    }

    fun stopVm() {
        viewModelScope.launch {
            _state.update { it.copy(vmStatus = VmStatus.Stopping, isLoading = true, error = null) }
            val profileName = activeProfileRepository.activeProfile.value
            val result = colimaService.stop(profileName)
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr.ifBlank { "Failed to stop Colima" }, isLoading = false) }
            }
            refreshStatus()
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun confirmDeleteVm() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }
    
    fun cancelDeleteVm() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }
    
    fun deleteVm() {
        val profileName = activeProfileRepository.activeProfile.value ?: "default"
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, showDeleteConfirmation = false) }
            try {
                // Must stop before delete
                if (_state.value.vmStatus == VmStatus.Running) {
                    colimaService.stop(profileName)
                }
                val result = colimaService.delete(profileName)
                if (!result.isSuccess()) {
                    _state.update { 
                        it.copy(
                            error = result.stderr.ifBlank { "Failed to delete VM" },
                            isLoading = false
                        ) 
                    }
                } else {
                    // Switch to default profile after deletion
                    activeProfileRepository.setActiveProfile(null)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to delete VM",
                        isLoading = false
                    ) 
                }
            }
            refreshStatus()
        }
    }
}

