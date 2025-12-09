package com.colimator.app.util

import com.colimator.app.service.ActiveProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeActiveProfileRepository : ActiveProfileRepository {
    private val _activeProfile = MutableStateFlow<String?>(null)
    override val activeProfile: StateFlow<String?> = _activeProfile.asStateFlow()
    
    private var _lastKnownProfile: String? = null
    
    override fun setActiveProfile(profileName: String?) {
        _lastKnownProfile = _activeProfile.value
        _activeProfile.value = profileName
    }
    
    override fun hasProfileChanged(): Boolean {
        return _activeProfile.value != _lastKnownProfile
    }
    
    override fun acknowledgeProfileChange() {
        _lastKnownProfile = _activeProfile.value
    }
    
    override fun getActiveProfileDisplayName(): String {
        return _activeProfile.value ?: "default"
    }
}
