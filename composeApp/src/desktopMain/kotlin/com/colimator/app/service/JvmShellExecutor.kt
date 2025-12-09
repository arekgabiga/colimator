package com.colimator.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
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
        var process: Process? = null
        try {
            val resolvedCommand = resolveExecutable(command)
            val pb = ProcessBuilder(listOf(resolvedCommand) + args)
            
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
            
            val newPath = if (existingPath.isNotEmpty()) "$commonPaths:$existingPath" else commonPaths
            env["PATH"] = newPath
            
            process = pb.start()
            
            // Close stdin immediately as we don't write to it. 
            // This prevents some commands from hanging waiting for input.
            process.outputStream.close()

            // Read output and error streams asynchronously to prevent deadlocks
            val stdoutDeferred = async { process!!.inputReader().readText() }
            val stderrDeferred = async { process!!.errorReader().readText() }

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            
            if (!finished) {
                process.destroyForcibly()
                return@withContext CommandResult(
                    exitCode = -1, 
                    stdout = try { stdoutDeferred.await() } catch (e: Exception) { "" },
                    stderr = "Timeout: Command did not complete within ${timeoutSeconds}s."
                )
            }

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()

            CommandResult(process.exitValue(), stdout, stderr)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ensure process is killed if we crash
            process?.destroyForcibly()
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
