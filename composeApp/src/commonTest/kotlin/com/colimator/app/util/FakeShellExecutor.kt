package com.colimator.app.util

import com.colimator.app.service.CommandResult
import com.colimator.app.service.ShellExecutor

class FakeShellExecutor : ShellExecutor {
    private val responses = mutableMapOf<String, CommandResult>()
    val executedCommands = mutableListOf<PluginCommand>()

    data class PluginCommand(val command: String, val args: List<String>)

    fun addResponse(command: String, args: List<String>, result: CommandResult) {
        val key = makeKey(command, args)
        responses[key] = result
    }

    // specific key generation simple logic
    private fun makeKey(command: String, args: List<String>): String {
        return "$command ${args.joinToString(" ")}"
    }

    override suspend fun execute(command: String, args: List<String>, timeoutSeconds: Long): CommandResult {
        executedCommands.add(PluginCommand(command, args))
        val key = makeKey(command, args)
        // Default to failure/empty if not mocked? Or throw?
        // Let's return a default failure so tests fail if we forget to mock.
        return responses[key] ?: CommandResult(exitCode = 127, stdout = "", stderr = "Command not mocked: $key")
    }

    override fun executeStream(
        command: String,
        args: List<String>,
        env: Map<String, String>
    ): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.emptyFlow()
}
