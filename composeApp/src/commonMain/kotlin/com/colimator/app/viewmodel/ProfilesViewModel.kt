package com.colimator.app.viewmodel

import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig
import com.colimator.app.model.MountType
import com.colimator.app.model.VmType
import com.colimator.app.service.ActiveProfileRepository
import com.colimator.app.service.ColimaService
import com.colimator.app.service.VmStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Profiles screen.
 */
data class ProfilesState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createConfig: ProfileCreateConfig = ProfileCreateConfig(
        name = "",
        cpuCores = 2,
        memoryGb = 2,
        diskGb = 60,
        kubernetes = false,
        vmType = VmType.VZ,
        mountType = MountType.VIRTIOFS
    ),
    val isCreating: Boolean = false,
    val profileToDelete: Profile? = null,
    val activeProfileName: String? = null
)

/**
 * ViewModel for Colima profile management.
 */
class ProfilesViewModel(
    private val colimaService: ColimaService,
    private val activeProfileRepository: ActiveProfileRepository
) : BaseViewModel() {
    
    private val _state = MutableStateFlow(ProfilesState())
    val state = _state.asStateFlow()
    
    init {
        // Subscribe to active profile changes
        viewModelScope.launch {
            activeProfileRepository.activeProfile.collect { profileName ->
                _state.update { it.copy(activeProfileName = profileName) }
            }
        }
        refreshProfiles()
    }
    
    fun refreshProfiles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val profiles = colimaService.listProfiles()
                _state.update { it.copy(profiles = profiles, isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to list profiles", 
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun showCreateDialog() {
        _state.update { 
            it.copy(
                showCreateDialog = true,
                createConfig = ProfileCreateConfig(
                    name = "",
                    cpuCores = 2,
                    memoryGb = 2,
                    diskGb = 60,
                    kubernetes = false,
                    vmType = VmType.VZ,
                    mountType = MountType.VIRTIOFS
                )
            ) 
        }
    }
    
    fun hideCreateDialog() {
        _state.update { it.copy(showCreateDialog = false) }
    }
    
    fun updateCreateConfig(config: ProfileCreateConfig) {
        _state.update { it.copy(createConfig = config) }
    }
    
    fun createProfile() {
        val config = _state.value.createConfig
        if (config.name.isBlank()) {
            _state.update { it.copy(error = "Profile name is required") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            try {
                val result = colimaService.start(config.name, config)
                if (result.isSuccess()) {
                    _state.update { it.copy(showCreateDialog = false, isCreating = false) }
                    refreshProfiles()
                } else {
                    _state.update { 
                        it.copy(
                            error = result.stderr.ifBlank { "Failed to create profile" },
                            isCreating = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to create profile",
                        isCreating = false
                    ) 
                }
            }
        }
    }
    
    fun startProfile(profile: Profile) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = colimaService.start(profile.name)
                if (!result.isSuccess()) {
                    _state.update { 
                        it.copy(error = result.stderr.ifBlank { "Failed to start profile" }) 
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to start profile") }
            }
            refreshProfiles()
        }
    }
    
    fun stopProfile(profile: Profile) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = colimaService.stop(profile.name)
                if (!result.isSuccess()) {
                    _state.update { 
                        it.copy(error = result.stderr.ifBlank { "Failed to stop profile" }) 
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to stop profile") }
            }
            refreshProfiles()
        }
    }
    
    fun switchToProfile(profile: Profile) {
        activeProfileRepository.setActiveProfile(
            if (profile.name == "default") null else profile.name
        )
        // If profile is not running, start it
        if (profile.status != VmStatus.Running) {
            startProfile(profile)
        }
    }
    
    fun confirmDeleteProfile(profile: Profile) {
        _state.update { it.copy(profileToDelete = profile) }
    }
    
    fun cancelDelete() {
        _state.update { it.copy(profileToDelete = null) }
    }
    
    fun deleteProfile() {
        val profile = _state.value.profileToDelete ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, profileToDelete = null) }
            try {
                // Must stop before delete
                if (profile.status == VmStatus.Running) {
                    colimaService.stop(profile.name)
                }
                val result = colimaService.delete(profile.name)
                if (!result.isSuccess()) {
                    _state.update { 
                        it.copy(error = result.stderr.ifBlank { "Failed to delete profile" }) 
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to delete profile") }
            }
            refreshProfiles()
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
