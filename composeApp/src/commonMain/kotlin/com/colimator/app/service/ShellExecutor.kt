package com.colimator.app.service

interface ShellExecutor {
    suspend fun execute(command: String, args: List<String>): CommandResult
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    fun isSuccess() = exitCode == 0
}
