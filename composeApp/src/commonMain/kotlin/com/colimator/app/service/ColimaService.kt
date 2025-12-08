package com.colimator.app.service

import kotlinx.serialization.SerialName
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
        // Use --json flag (not --output json) for JSON output
        val result = shell.execute("colima", listOf("status", "--json"), QUICK_TIMEOUT)
        // When colima is running, status returns exit code 0
        // When colima is stopped, status returns exit code 1
        return if (result.isSuccess()) VmStatus.Running else VmStatus.Stopped
    }
    
    /**
     * Get detailed VM configuration. Returns null if VM is not running.
     */
    suspend fun getConfig(): ColimaConfig? {
        val result = shell.execute("colima", listOf("status", "--json"), QUICK_TIMEOUT)
        if (!result.isSuccess()) return null
        
        return try {
            val status = json.decodeFromString<ColimaStatusJson>(result.stdout)
            ColimaConfig(
                cpuCores = status.cpu,
                memoryBytes = status.memory,
                diskBytes = status.disk,
                architecture = status.arch,
                runtime = status.runtime,
                driver = status.driver,
                mountType = status.mountType,
                kubernetes = status.kubernetes
            )
        } catch (e: Exception) {
            null
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

/**
 * VM configuration details.
 */
data class ColimaConfig(
    val cpuCores: Int,
    val memoryBytes: Long,
    val diskBytes: Long,
    val architecture: String,
    val runtime: String,
    val driver: String,
    val mountType: String,
    val kubernetes: Boolean
) {
    /** Memory in gigabytes */
    val memoryGb: Double get() = memoryBytes / 1_073_741_824.0
    
    /** Disk in gigabytes */
    val diskGb: Double get() = diskBytes / 1_073_741_824.0
}

/**
 * JSON structure for colima status --json output.
 */
@Serializable
internal data class ColimaStatusJson(
    val cpu: Int = 0,
    val memory: Long = 0,
    val disk: Long = 0,
    val arch: String = "",
    val runtime: String = "",
    val driver: String = "",
    @SerialName("mount_type") val mountType: String = "",
    val kubernetes: Boolean = false
)
