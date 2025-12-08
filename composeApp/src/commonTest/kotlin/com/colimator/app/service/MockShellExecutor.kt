package com.colimator.app.service

/**
 * Mock ShellExecutor for testing that returns predefined results.
 */
class MockShellExecutor : ShellExecutor {
    
    private val responses = mutableMapOf<Pair<String, List<String>>, CommandResult>()
    private val calls = mutableListOf<Pair<String, List<String>>>()
    
    /**
     * Configure a mock response for a specific command.
     */
    fun mockResponse(command: String, args: List<String>, result: CommandResult) {
        responses[command to args] = result
    }
    
    /**
     * Get all recorded calls.
     */
    fun getCalls(): List<Pair<String, List<String>>> = calls.toList()
    
    /**
     * Clear all recorded calls.
     */
    fun clearCalls() {
        calls.clear()
    }
    
    override suspend fun execute(
        command: String, 
        args: List<String>,
        timeoutSeconds: Long
    ): CommandResult {
        calls.add(command to args)
        return responses[command to args] 
            ?: CommandResult(-1, "", "No mock response configured for: $command $args")
    }
}
