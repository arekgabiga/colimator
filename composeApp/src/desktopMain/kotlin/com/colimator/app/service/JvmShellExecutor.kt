package com.colimator.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun executeStream(
        command: String,
        args: List<String>,
        env: Map<String, String>
    ): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
        var process: Process? = null
        try {
            val resolvedCommand = resolveExecutable(command)
            val pb = ProcessBuilder(listOf(resolvedCommand) + args)
            
            // Fix for GUI apps not loading .zshrc/.bash_profile PATH
            // Code duplicated from execute() as requested to ensure consistency
            val pbEnv = pb.environment()
            val existingPath = pbEnv["PATH"] ?: ""
            val commonPaths = listOf(
                "/usr/local/bin",
                "/opt/homebrew/bin",
                "/opt/local/bin",
                System.getProperty("user.home") + "/.colima/bin",
                System.getProperty("user.home") + "/.docker/bin"
            ).joinToString(":")
            
            val newPath = if (existingPath.isNotEmpty()) "$commonPaths:$existingPath" else commonPaths
            pbEnv["PATH"] = newPath
            
            // Apply custom environment variables
            pbEnv.putAll(env)
            
            process = pb.start()
            
            // Close stdin immediately
            process.outputStream.close()
            
            val stdoutReader = process.inputStream.bufferedReader()
            val stderrReader = process.errorStream.bufferedReader()
            
            // We want to read both streams. Since we are in a Flow, we can emit strings from both.
            val channel = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            
            val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
            
            val stdoutJob = scope.launch {
                try {
                    while (true) {
                        val line = stdoutReader.readLine() ?: break
                        channel.send(line)
                    }
                } catch (e: Exception) {
                    // Ignore stream closed errors
                }
            }
            
            val stderrJob = scope.launch {
                try {
                    while (true) {
                        val line = stderrReader.readLine() ?: break
                        channel.send(line)
                    }
                } catch (e: Exception) {
                     // Ignore stream closed errors
                }
            }
            
            try {
                // Emit lines as they come
                while (process.isAlive || !channel.isEmpty) {
                    val line = channel.tryReceive().getOrNull()
                    if (line != null) {
                        emit(line)
                    } else {
                        // Small delay to prevent tight loop if process is running but no output yet
                        kotlinx.coroutines.delay(10)
                        
                        // If both jobs finished and channel empty, break
                        if (stdoutJob.isCompleted && stderrJob.isCompleted && channel.isEmpty) break
                    }
                }
            } finally {
                stdoutJob.cancel()
                stderrJob.cancel()
                channel.close()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            emit("Error: ${e.message}")
        } finally {
            process?.destroyForcibly()
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
