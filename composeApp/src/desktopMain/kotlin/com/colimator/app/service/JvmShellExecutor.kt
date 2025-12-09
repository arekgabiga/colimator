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
            val resolvedCommand = resolveExecutable(command)
            val pb = ProcessBuilder(listOf(resolvedCommand) + args)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
            
            // Fix for GUI apps not loading .zshrc/.bash_profile PATH
            val env = pb.environment()
            val existingPath = env["PATH"] ?: ""
            val commonPaths = listOf(
                "/usr/local/bin",
                "/opt/homebrew/bin",
                "/opt/local/bin",
                System.getProperty("user.home") + "/.colima/bin",
                System.getProperty("user.home") + "/.docker/bin"
            ).joinToString(":")
            
            env["PATH"] = if (existingPath.isNotEmpty()) "$commonPaths:$existingPath" else commonPaths
            
            val process = pb.start()

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
            e.printStackTrace()
            CommandResult(-1, "", "Exception: ${e.message}")
        }
    }

    private fun resolveExecutable(command: String): String {
        if (command.contains("/")) return command // Already absolute or relative path
        
        val searchPaths = listOf(
            "/usr/local/bin",
            "/opt/homebrew/bin",
            "/opt/local/bin",
            System.getProperty("user.home") + "/.colima/bin",
            System.getProperty("user.home") + "/.docker/bin",
            "/usr/bin",
            "/bin",
            "/usr/sbin",
            "/sbin"
        )
        
        for (path in searchPaths) {
            val file = java.io.File(path, command)
            if (file.exists() && file.canExecute()) {
                return file.absolutePath
            }
        }
        
        return command // Fallback to system lookup
    }
}
