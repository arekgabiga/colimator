package com.colimator.app.service

import com.colimator.app.model.Container
import com.colimator.app.model.DockerImage
import kotlinx.serialization.json.Json

/**
 * Service for Docker CLI operations.
 */
class DockerService(private val shell: ShellExecutor) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Get the Docker context name for a Colima profile.
     * - Default profile (null or "default") uses context "colima"
     * - Named profiles use context "colima-<name>"
     */
    private fun getContextForProfile(profileName: String?): String {
        return if (profileName == null || profileName == "default") {
            "colima"
        } else {
            "colima-$profileName"
        }
    }
    
    /**
     * Build command arguments with --context flag prepended.
     */
    private fun withContext(profileName: String?, args: List<String>): List<String> {
        val context = getContextForProfile(profileName)
        return listOf("--context", context) + args
    }
    
    /**
     * Check if docker CLI is installed and accessible.
     */
    suspend fun isInstalled(): Boolean {
        val result = shell.execute("docker", listOf("--version"))
        return result.isSuccess()
    }
    
    /**
     * List all containers (running and stopped).
     * Parses output from 'docker ps -a --format json'.
     */
    suspend fun listContainers(profileName: String? = null): List<Container> {
        val args = withContext(profileName, listOf("ps", "-a", "--format", "json"))
        val result = shell.execute("docker", args)
        if (!result.isSuccess()) {
            return emptyList()
        }
        
        // Docker outputs one JSON object per line
        return result.stdout
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    json.decodeFromString<Container>(line)
                } catch (e: Exception) {
                    null // Skip malformed lines
                }
            }
    }
    
    /**
     * List all Docker images.
     * Parses output from 'docker images --format json'.
     */
    suspend fun listImages(profileName: String? = null): List<DockerImage> {
        val args = withContext(profileName, listOf("images", "--format", "json"))
        val result = shell.execute("docker", args)
        if (!result.isSuccess()) {
            return emptyList()
        }
        
        // Docker outputs one JSON object per line
        return result.stdout
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    json.decodeFromString<DockerImage>(line)
                } catch (e: Exception) {
                    null // Skip malformed lines
                }
            }
    }
    
    /**
     * Start a stopped container.
     */
    suspend fun startContainer(id: String, profileName: String? = null): CommandResult {
        val args = withContext(profileName, listOf("start", id))
        return shell.execute("docker", args)
    }
    
    /**
     * Stop a running container.
     */
    suspend fun stopContainer(id: String, profileName: String? = null): CommandResult {
        val args = withContext(profileName, listOf("stop", id))
        return shell.execute("docker", args)
    }
    
    /**
     * Remove a container (must be stopped first).
     */
    suspend fun removeContainer(id: String, profileName: String? = null): CommandResult {
        val args = withContext(profileName, listOf("rm", id))
        return shell.execute("docker", args)
    }
}
