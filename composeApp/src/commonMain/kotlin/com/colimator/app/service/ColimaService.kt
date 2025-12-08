package com.colimator.app.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ColimaService(private val shell: ShellExecutor) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun isInstalled(): Boolean {
        // Simple check: 'which colima' or 'colima --version'
        val result = shell.execute("colima", listOf("--version"))
        return result.isSuccess()
    }

    suspend fun getStatus(): VmStatus {
        // Try json format first
        val result = shell.execute("colima", listOf("status", "--output", "json"))
        if (result.isSuccess()) {
            return try {
                val status = json.decodeFromString<ColimaStatusJson>(result.stdout)
                if (status.running) VmStatus.Running else VmStatus.Stopped
            } catch (e: Exception) {
                // Fallback or log error
                VmStatus.Unknown
            }
        } else {
            // If colima is not running, it might return exit code 1 or just "not running"
            return VmStatus.Stopped
        }
    }

    suspend fun start() = shell.execute("colima", listOf("start"))
    suspend fun stop() = shell.execute("colima", listOf("stop"))
}

enum class VmStatus {
    Running, Stopped, Starting, Stopping, Unknown
}

@Serializable
data class ColimaStatusJson(
    val running: Boolean
)
