package com.colimator.app.service

/**
 * Interface for executing shell commands.
 */
interface ShellExecutor {
    /**
     * Execute a command with optional timeout.
     * @param command The command to execute
     * @param args Arguments for the command
     * @param timeoutSeconds Timeout in seconds (default 60)
     */
    suspend fun execute(
        command: String, 
        args: List<String>, 
        timeoutSeconds: Long = 60
    ): CommandResult
}

/**
 * Result of a shell command execution.
 */
data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    fun isSuccess() = exitCode == 0
    
    val isTimeout: Boolean
        get() = exitCode == -1 && stderr.startsWith("Timeout:")
}
