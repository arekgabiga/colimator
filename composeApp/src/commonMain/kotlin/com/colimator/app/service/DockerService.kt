package com.colimator.app.service

import com.colimator.app.model.Container
import kotlinx.serialization.json.Json

/**
 * Service for Docker CLI operations.
 */
class DockerService(private val shell: ShellExecutor) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
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
    suspend fun listContainers(): List<Container> {
        val result = shell.execute("docker", listOf("ps", "-a", "--format", "json"))
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
     * Start a stopped container.
     */
    suspend fun startContainer(id: String): CommandResult {
        return shell.execute("docker", listOf("start", id))
    }
    
    /**
     * Stop a running container.
     */
    suspend fun stopContainer(id: String): CommandResult {
        return shell.execute("docker", listOf("stop", id))
    }
    
    /**
     * Remove a container (must be stopped first).
     */
    suspend fun removeContainer(id: String): CommandResult {
        return shell.execute("docker", listOf("rm", id))
    }
}
