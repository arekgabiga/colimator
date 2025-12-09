package com.colimator.app.util

import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig
import com.colimator.app.service.ColimaConfig
import com.colimator.app.service.ColimaService
import com.colimator.app.service.CommandResult
import com.colimator.app.service.VmStatus

class FakeColimaService : ColimaService {
    var isInstalled = true
    var profiles = listOf<Profile>()
    var vmStatus = VmStatus.Running
    var colimaConfig: ColimaConfig? = null
    
    // Command tracking
    val executedCommands = mutableListOf<String>()

    override suspend fun isInstalled(): Boolean = isInstalled

    override suspend fun listProfiles(): List<Profile> = profiles

    override suspend fun getStatus(profileName: String?): VmStatus = vmStatus

    override suspend fun getConfig(profileName: String?): ColimaConfig? = colimaConfig

    override suspend fun start(profileName: String?, config: ProfileCreateConfig?): CommandResult {
        executedCommands.add("start $profileName")
        return CommandResult(0, "", "")
    }

    override suspend fun stop(profileName: String?): CommandResult {
        executedCommands.add("stop $profileName")
        return CommandResult(0, "", "")
    }

    override suspend fun delete(profileName: String): CommandResult {
        executedCommands.add("delete $profileName")
        return CommandResult(0, "", "")
    }
}
