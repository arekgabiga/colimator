package com.colimator.app.viewmodel

import com.colimator.app.model.Container
import com.colimator.app.service.DockerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the containers list screen.
 */
data class ContainersState(
    val containers: List<Container> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing Docker container list and operations.
 */
class ContainersViewModel(private val dockerService: DockerService) : BaseViewModel() {
    
    private val _state = MutableStateFlow(ContainersState())
    val state: StateFlow<ContainersState> = _state.asStateFlow()
    
    init {
        refresh()
    }
    
    /**
     * Refresh the container list from Docker.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val containers = dockerService.listContainers()
                _state.update { it.copy(containers = containers, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load containers", isLoading = false) }
            }
        }
    }
    
    /**
     * Start a stopped container.
     */
    fun startContainer(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = dockerService.startContainer(id)
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr, isLoading = false) }
            } else {
                refresh()
            }
        }
    }
    
    /**
     * Stop a running container.
     */
    fun stopContainer(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = dockerService.stopContainer(id)
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr, isLoading = false) }
            } else {
                refresh()
            }
        }
    }
    
    /**
     * Remove a container (must be stopped first).
     */
    fun removeContainer(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = dockerService.removeContainer(id)
            if (!result.isSuccess()) {
                _state.update { it.copy(error = result.stderr, isLoading = false) }
            } else {
                refresh()
            }
        }
    }
    
    /**
     * Clear any displayed error.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
