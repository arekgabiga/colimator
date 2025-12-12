package com.colimator.app.service

import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig

/**
 * Fake implementation of [ColimaService] for testing.
 */
class FakeColimaService : ColimaService {

    private var _isInstalled: Boolean = true
    private val _profiles = mutableListOf<Profile>()
    private var _config: ColimaConfig? = null
    
    // For specialized test behavior
    var startResult: CommandResult = CommandResult(0, "", "")
    var stopResult: CommandResult = CommandResult(0, "", "")
    var deleteResult: CommandResult = CommandResult(0, "", "")
    
    var simulationDelay: Long = 0L

    fun setInstalled(installed: Boolean) {
        _isInstalled = installed
    }

    fun setProfiles(profiles: List<Profile>) {
        _profiles.clear()
        _profiles.addAll(profiles)
    }
    
    fun setConfig(config: ColimaConfig?) {
        _config = config
    }

    override suspend fun isInstalled(): Boolean {
        return _isInstalled
    }

    override suspend fun listProfiles(): List<Profile> {
        return _profiles
    }

    override suspend fun getStatus(profileName: String?): VmStatus {
        val target = profileName ?: "default"
        return _profiles.find { it.name == target }?.status ?: VmStatus.Stopped
    }

    override suspend fun getConfig(profileName: String?): ColimaConfig? {
        // In a real fake, we might maintain a map of configs per profile.
        // For simple tests, returning a single settable config is often enough 
        // if we assume we are testing one active profile.
        // Or we can check if the profile exists and is running.
        val target = profileName ?: "default"
        val profile = _profiles.find { it.name == target }
        
        if (profile?.status == VmStatus.Running) {
            return _config
        }
        return null
    }

    override suspend fun start(profileName: String?, config: ProfileCreateConfig?): CommandResult {
        if (simulationDelay > 0) kotlinx.coroutines.delay(simulationDelay)
        if (!startResult.isSuccess()) return startResult
        
        val target = profileName ?: "default"
        // Update profile status to Running
        val existing = _profiles.find { it.name == target }
        if (existing != null) {
            val index = _profiles.indexOf(existing)
            _profiles[index] = existing.copy(status = VmStatus.Running, isActive = true)
        } else {
            // Create new if strictly needed? Or assume start only works on existing for now unless config provided?
            // "start" creates if it doesn't exist.
            _profiles.add(
                Profile(
                    name = target, 
                    status = VmStatus.Running, 
                    isActive = true,
                    cpuCores = config?.cpu ?: 2,
                    memoryBytes = (config?.memory ?: 2).toLong() * 1024 * 1024 * 1024,
                    diskBytes = (config?.disk ?: 60).toLong() * 1024 * 1024 * 1024
                )
            )
        }
        return startResult
    }

    override suspend fun stop(profileName: String?): CommandResult {
        if (simulationDelay > 0) kotlinx.coroutines.delay(simulationDelay)
        if (!stopResult.isSuccess()) return stopResult

        val target = profileName ?: "default"
        val existing = _profiles.find { it.name == target }
        if (existing != null) {
            val index = _profiles.indexOf(existing)
            _profiles[index] = existing.copy(status = VmStatus.Stopped, isActive = false)
        }
        return stopResult
    }

    override suspend fun delete(profileName: String): CommandResult {
        if (simulationDelay > 0) kotlinx.coroutines.delay(simulationDelay)
        if (!deleteResult.isSuccess()) return deleteResult

        _profiles.removeIf { it.name == profileName }
        return deleteResult
    }
}
