package com.colimator.app.service

import com.colimator.app.model.MountType
import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig
import com.colimator.app.model.VmType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for Colima VM operations.
 */
interface ColimaService {
    suspend fun isInstalled(): Boolean
    suspend fun listProfiles(): List<Profile>
    suspend fun getStatus(profileName: String? = null): VmStatus
    suspend fun getConfig(profileName: String? = null): ColimaConfig?
    suspend fun start(profileName: String? = null, config: ProfileCreateConfig? = null): CommandResult
    suspend fun stop(profileName: String? = null): CommandResult
    suspend fun delete(profileName: String): CommandResult
}

/**
 * Service for Colima VM operations.
 */
class ColimaServiceImpl(private val shell: ShellExecutor) : ColimaService {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        /** Timeout for quick commands like status, version */
        private const val QUICK_TIMEOUT = 30L
        
        /** Timeout for start command (can take minutes for first boot or image pull) */
        private const val START_TIMEOUT = 300L
        
        /** Timeout for stop command */
        private const val STOP_TIMEOUT = 60L
        
        /** Timeout for delete command */
        private const val DELETE_TIMEOUT = 30L
    }

    override suspend fun isInstalled(): Boolean {
        val result = shell.execute("colima", listOf("--version"), QUICK_TIMEOUT)
        return result.isSuccess()
    }

    /**
     * List all Colima profiles.
     */
    override suspend fun listProfiles(): List<Profile> {
        val result = shell.execute("colima", listOf("list", "--json"), QUICK_TIMEOUT)
        if (!result.isSuccess()) return emptyList()
        
        return try {
            // colima list --json outputs one JSON object per line
            result.stdout
                .lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val entry = json.decodeFromString<ColimaListEntryJson>(line)
                        Profile(
                            name = entry.name,
                            status = parseStatus(entry.status),
                            cpuCores = entry.cpu,
                            memoryBytes = entry.memory,
                            diskBytes = entry.disk,
                            isActive = entry.status.lowercase() == "running"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseStatus(status: String): VmStatus {
        return when (status.lowercase()) {
            "running" -> VmStatus.Running
            "stopped" -> VmStatus.Stopped
            "starting" -> VmStatus.Starting
            "stopping" -> VmStatus.Stopping
            else -> VmStatus.Unknown
        }
    }

    /**
     * Get status for a specific profile.
     */
    override suspend fun getStatus(profileName: String?): VmStatus {
        val targetName = profileName ?: "default"
        val profiles = listProfiles()
        return profiles.find { it.name == targetName }?.status ?: VmStatus.Stopped
    }
    
    /**
     * Get detailed VM configuration for a specific profile.
     * Returns null if VM is not running.
     */
    override suspend fun getConfig(profileName: String?): ColimaConfig? {
        val args = buildList {
            add("status")
            add("--json")
            profileName?.let { 
                add("--profile")
                add(it)
            }
        }
        val result = shell.execute("colima", args, QUICK_TIMEOUT)
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

    /**
     * Start a profile. Creates new profile if config provided and profile doesn't exist.
     */
    override suspend fun start(profileName: String?, config: ProfileCreateConfig?): CommandResult {
        val args = buildList {
            add("start")
            profileName?.let { 
                add("--profile")
                add(it)
            }
            config?.let { cfg ->
                add("--cpu")
                add(cfg.cpu.toString())
                add("--memory")
                add(cfg.memory.toString())
                add("--disk")
                add(cfg.disk.toString())
                add("--vm-type")
                add(cfg.vmType.cliValue)
                add("--mount-type")
                add(cfg.mountType.cliValue)
                if (cfg.kubernetes) {
                    add("--kubernetes")
                }
            }
        }
        return shell.execute("colima", args, START_TIMEOUT)
    }
    
    /**
     * Stop a profile.
     */
    override suspend fun stop(profileName: String?): CommandResult {
        val args = buildList {
            add("stop")
            profileName?.let { 
                add("--profile")
                add(it)
            }
        }
        return shell.execute("colima", args, STOP_TIMEOUT)
    }
    
    /**
     * Delete a profile. Profile must be stopped first.
     */
    override suspend fun delete(profileName: String): CommandResult {
        val args = listOf("delete", "--profile", profileName, "--force")
        return shell.execute("colima", args, DELETE_TIMEOUT)
    }
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

/**
 * JSON structure for colima list --json output (one entry per line).
 */
@Serializable
internal data class ColimaListEntryJson(
    val name: String = "default",
    val status: String = "Stopped",
    val cpu: Int = 0,
    val memory: Long = 0,
    val disk: Long = 0,
    val arch: String = "",
    val runtime: String = ""
)

