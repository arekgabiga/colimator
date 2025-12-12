package com.colimator.app.viewmodel

import com.colimator.app.model.Container
import com.colimator.app.service.ActiveProfileRepository
import com.colimator.app.service.DockerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sort field options for containers list.
 */
enum class ContainerSortField { NAME, STATUS }

/**
 * State for the containers list screen.
 */
data class ContainersState(
    val containers: List<Container> = emptyList(),
    val sortField: ContainerSortField = ContainerSortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortVersion: Int = 0,  // Incremented on sort change to trigger scroll reset
    val activeProfile: String = "default"
)

/**
 * ViewModel for managing Docker container list and operations.
 */
class ContainersViewModel(
    private val dockerService: DockerService,
    private val activeProfileRepository: ActiveProfileRepository
) : BaseViewModel() {
    
    private val _state = MutableStateFlow(ContainersState())
    val state: StateFlow<ContainersState> = _state.asStateFlow()
    
    init {
        // Subscribe to profile changes
        viewModelScope.launch {
            activeProfileRepository.activeProfile.collect { profile ->
                val profileName = profile ?: "default"
                // Clear existing data immediately when profile changes
                _state.update { 
                    it.copy(
                        containers = emptyList(), 
                        activeProfile = profileName
                    ) 
                }
                // Refresh for new profile
                refresh()
            }
        }
    }
    
    /**
     * Called when screen becomes visible. Forces refresh if profile changed.
     */
    fun onScreenVisible() {
        if (activeProfileRepository.hasProfileChanged()) {
            _state.update { it.copy(containers = emptyList()) }
            refresh()
            activeProfileRepository.acknowledgeProfileChange()
        }
    }
    
    /**
     * Refresh the container list from Docker.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val profileName = activeProfileRepository.activeProfile.value
                val containers = dockerService.listContainers(profileName)
                val sorted = sortContainers(containers, _state.value.sortField, _state.value.sortDirection)
                _state.update { it.copy(containers = sorted, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load containers", isLoading = false) }
            }
        }
    }
    
    /**
     * Set sort field. Toggles direction if same field, resets to ASC if different.
     */
    fun setSortField(field: ContainerSortField) {
        _state.update { current ->
            val newDirection = if (current.sortField == field) {
                if (current.sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            } else {
                SortDirection.ASC
            }
            val sorted = sortContainers(current.containers, field, newDirection)
            current.copy(
                containers = sorted,
                sortField = field,
                sortDirection = newDirection,
                sortVersion = current.sortVersion + 1
            )
        }
    }
    
    private fun sortContainers(
        containers: List<Container>,
        field: ContainerSortField,
        direction: SortDirection
    ): List<Container> {
        val comparator: Comparator<Container> = when (field) {
            ContainerSortField.NAME -> compareBy { it.displayName.lowercase() }
            ContainerSortField.STATUS -> compareBy { !it.isRunning } // Running first when ASC
        }
        return if (direction == SortDirection.ASC) {
            containers.sortedWith(comparator)
        } else {
            containers.sortedWith(comparator.reversed())
        }
    }
    
    /**
     * Start a stopped container.
     */
    fun startContainer(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val profileName = activeProfileRepository.activeProfile.value
            val result = dockerService.startContainer(id, profileName)
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
            val profileName = activeProfileRepository.activeProfile.value
            val result = dockerService.stopContainer(id, profileName)
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
            val profileName = activeProfileRepository.activeProfile.value
            val result = dockerService.removeContainer(id, profileName)
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
    
    // --- Logging Support ---
    
    private val _logs = MutableStateFlow<List<com.colimator.app.model.LogLine>>(emptyList())
    val logs: StateFlow<List<com.colimator.app.model.LogLine>> = _logs.asStateFlow()
    
    private var logJob: kotlinx.coroutines.Job? = null
    private val maxLogLines = 5000
    
    fun startStreamingLogs(containerId: String) {
        stopStreamingLogs()
        _logs.value = emptyList()
        
        logJob = viewModelScope.launch {
            val profileName = activeProfileRepository.activeProfile.value
            dockerService.streamLogs(containerId, profileName)
                .collect { rawLine ->
                    // Expected format: "2023-10-27T10:00:00.000000000Z message content..."
                    // Docker with -t adds timestamp + space at the beginning
                    val parts = rawLine.split(" ", limit = 2)
                    var timestamp = ""
                    var content = rawLine
                    
                    if (parts.size == 2 && parts[0].length > 20) { // Rough check for timestamp
                         timestamp = parts[0]
                         content = parts[1]
                    }
                    
                    val logLine = com.colimator.app.model.LogLine(timestamp, content)
                    
                    _logs.update { current ->
                        val newList = current + logLine
                        if (newList.size > maxLogLines) {
                             newList.drop(newList.size - maxLogLines)
                        } else {
                            newList
                        }
                    }
                }
        }
    }
    
    fun stopStreamingLogs() {
        logJob?.cancel()
        logJob = null
    }
}

