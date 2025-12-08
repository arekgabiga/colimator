package com.colimator.app.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for Colima VM operations.
 */
class ColimaService(private val shell: ShellExecutor) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        /** Timeout for quick commands like status, version */
        private const val QUICK_TIMEOUT = 30L
        
        /** Timeout for start command (can take minutes for first boot or image pull) */
        private const val START_TIMEOUT = 180L
        
        /** Timeout for stop command */
        private const val STOP_TIMEOUT = 60L
    }

    suspend fun isInstalled(): Boolean {
        val result = shell.execute("colima", listOf("--version"), QUICK_TIMEOUT)
        return result.isSuccess()
    }

    suspend fun getStatus(): VmStatus {
        val result = shell.execute("colima", listOf("status", "--output", "json"), QUICK_TIMEOUT)
        if (result.isSuccess()) {
            return try {
                val status = json.decodeFromString<ColimaStatusJson>(result.stdout)
                if (status.running) VmStatus.Running else VmStatus.Stopped
            } catch (e: Exception) {
                VmStatus.Unknown
            }
        } else {
            // If colima is not running, it returns exit code 1
            return VmStatus.Stopped
        }
    }

    suspend fun start(): CommandResult = 
        shell.execute("colima", listOf("start"), START_TIMEOUT)
    
    suspend fun stop(): CommandResult = 
        shell.execute("colima", listOf("stop"), STOP_TIMEOUT)
}

enum class VmStatus {
    Running, Stopped, Starting, Stopping, Unknown
}

@Serializable
data class ColimaStatusJson(
    val running: Boolean
)
