package com.colimator.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class JvmShellExecutor : ShellExecutor {
    override suspend fun execute(command: String, args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(listOf(command) + args)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            // Wait for the process to finish with a timeout (e.g., 30 seconds for now)
            // For long running tasks like 'colima start', we might need a longer timeout or streaming
            // For MVP managed commands, we'll try to wait.
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            
            val stdout = process.inputReader().readText()
            val stderr = process.errorReader().readText()

            if (!finished) {
                process.destroy()
                return@withContext CommandResult(-1, stdout, "Timeout: $stderr")
            }

            CommandResult(process.exitValue(), stdout, stderr)
        } catch (e: Exception) {
            CommandResult(-1, "", "Exception: ${e.message}")
        }
    }
}
