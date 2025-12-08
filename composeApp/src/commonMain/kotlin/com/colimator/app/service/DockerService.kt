package com.colimator.app.service

/**
 * Service for Docker CLI operations.
 */
class DockerService(private val shell: ShellExecutor) {
    
    /**
     * Check if docker CLI is installed and accessible.
     */
    suspend fun isInstalled(): Boolean {
        val result = shell.execute("docker", listOf("--version"))
        return result.isSuccess()
    }
}
