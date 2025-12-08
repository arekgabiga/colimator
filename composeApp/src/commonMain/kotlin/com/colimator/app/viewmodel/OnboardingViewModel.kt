package com.colimator.app.viewmodel

import com.colimator.app.service.ColimaService
import com.colimator.app.service.DockerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for onboarding dependency check.
 */
sealed class OnboardingState {
    data object Checking : OnboardingState()
    data object Ready : OnboardingState()
    data class Missing(
        val colimaInstalled: Boolean,
        val dockerInstalled: Boolean
    ) : OnboardingState() {
        val message: String
            get() = buildList {
                if (!colimaInstalled) add("Colima")
                if (!dockerInstalled) add("Docker")
            }.joinToString(" and ") + " not found.\n\nPlease install the missing dependencies and restart."
    }
}

/**
 * ViewModel for checking CLI dependencies on app startup.
 */
class OnboardingViewModel(
    private val colimaService: ColimaService,
    private val dockerService: DockerService
) : BaseViewModel() {
    
    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Checking)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    
    init {
        checkDependencies()
    }
    
    fun checkDependencies() {
        viewModelScope.launch {
            _state.update { OnboardingState.Checking }
            
            val colimaInstalled = colimaService.isInstalled()
            val dockerInstalled = dockerService.isInstalled()
            
            _state.update {
                if (colimaInstalled && dockerInstalled) {
                    OnboardingState.Ready
                } else {
                    OnboardingState.Missing(colimaInstalled, dockerInstalled)
                }
            }
        }
    }
}
