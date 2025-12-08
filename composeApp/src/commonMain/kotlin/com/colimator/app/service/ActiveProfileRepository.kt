package com.colimator.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository to track the currently active Colima profile.
 * Shared across ViewModels to coordinate profile switching.
 */
class ActiveProfileRepository {
    private val _activeProfile = MutableStateFlow<String?>(null)
    val activeProfile: StateFlow<String?> = _activeProfile.asStateFlow()
    
    private var _lastKnownProfile: String? = null
    
    /**
     * Set the active profile name.
     * @param profileName The profile name, or null for default profile.
     */
    fun setActiveProfile(profileName: String?) {
        _lastKnownProfile = _activeProfile.value
        _activeProfile.value = profileName
    }
    
    /**
     * Check if the profile has changed since the last check.
     * Useful for forcing refresh when navigating to screens.
     */
    fun hasProfileChanged(): Boolean {
        return _activeProfile.value != _lastKnownProfile
    }
    
    /**
     * Mark that the current profile state has been acknowledged.
     * Call this after refreshing data to reset the changed flag.
     */
    fun acknowledgeProfileChange() {
        _lastKnownProfile = _activeProfile.value
    }
    
    /**
     * Get the display name of the active profile.
     */
    fun getActiveProfileDisplayName(): String {
        return _activeProfile.value ?: "default"
    }
}
