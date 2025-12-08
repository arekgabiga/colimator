package com.colimator.app.service

/**
 * Service for Colima VM operations.
 */
class ColimaService(private val shell: ShellExecutor) {
    
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

    suspend fun start(): CommandResult = 
        shell.execute("colima", listOf("start"), START_TIMEOUT)
    
    suspend fun stop(): CommandResult = 
        shell.execute("colima", listOf("stop"), STOP_TIMEOUT)
}

enum class VmStatus {
    Running, Stopped, Starting, Stopping, Unknown
}
