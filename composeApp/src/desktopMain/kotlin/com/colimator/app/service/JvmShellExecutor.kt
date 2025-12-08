package com.colimator.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * JVM implementation of ShellExecutor using ProcessBuilder.
 */
class JvmShellExecutor : ShellExecutor {
    
    override suspend fun execute(
        command: String, 
        args: List<String>,
        timeoutSeconds: Long
    ): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(listOf(command) + args)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            
            val stdout = process.inputReader().readText()
            val stderr = process.errorReader().readText()

            if (!finished) {
                process.destroyForcibly()
                return@withContext CommandResult(
                    exitCode = -1, 
                    stdout = stdout, 
                    stderr = "Timeout: Command did not complete within ${timeoutSeconds}s. $stderr"
                )
            }

            CommandResult(process.exitValue(), stdout, stderr)
        } catch (e: Exception) {
            CommandResult(-1, "", "Exception: ${e.message}")
        }
    }
}
