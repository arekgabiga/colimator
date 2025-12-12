package com.colimator.app.service

import com.colimator.app.domain.ColimaConfig
import com.colimator.app.domain.HardwareSpecs
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

actual object ConfigurationService {

    private val userHome = System.getProperty("user.home")
    // Visible for testing
    var colimaDir = File(userHome, ".colima")

    actual fun loadConfig(profileName: String): ColimaConfig {
        val configFile = getConfigFile(profileName)
        if (!configFile.exists()) {
            return ColimaConfig() // Return defaults if no config
        }

        return try {
            val yaml = Yaml()
            val data = FileInputStream(configFile).use { yaml.load<Map<String, Any>>(it) }
            
            ColimaConfig(
                cpu = (data["cpu"] as? Number)?.toInt() ?: 2,
                memory = (data["memory"] as? Number)?.toInt() ?: 2,
                disk = (data["disk"] as? Number)?.toInt() ?: 60,
                kubernetes = (data["kubernetes"] as? Map<*, *>)?.get("enabled") as? Boolean ?: false,
                runtime = (data["runtime"] as? String) ?: "docker"
            )
        } catch (e: Exception) {
            System.err.println("Warning: Failed to parse config for profile '$profileName': ${e.message}")
            ColimaConfig() // Fallback
        }
    }

    actual fun saveConfig(profileName: String, config: ColimaConfig) {
        val configFile = getConfigFile(profileName)
        // Ensure directory exists (it should for existing profiles)
        configFile.parentFile.mkdirs()

        val yaml = Yaml(DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        })

        val existingData: MutableMap<String, Any> = if (configFile.exists()) {
            try {
                FileInputStream(configFile).use { yaml.load<Map<String, Any>>(it) }?.toMutableMap() ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }

        // Update fields
        existingData["cpu"] = config.cpu
        existingData["memory"] = config.memory
        existingData["disk"] = config.disk
        
        @Suppress("UNCHECKED_CAST")
        val k8sConfig = (existingData["kubernetes"] as? MutableMap<String, Any>) ?: mutableMapOf()
        k8sConfig["enabled"] = config.kubernetes
        existingData["kubernetes"] = k8sConfig

        // Persist
        FileWriter(configFile).use { writer ->
            yaml.dump(existingData, writer)
        }
    }

    private fun getConfigFile(profileName: String): File {
        // Colima structure: ~/.colima/default/colima.yaml
        return File(colimaDir, "$profileName/colima.yaml")
    }

    actual fun getHostHardwareSpecs(): HardwareSpecs {
         return try {
            val memProcess = ProcessBuilder("sysctl", "-n", "hw.memsize").start()
            val memBytes = String(memProcess.inputStream.readAllBytes()).trim().toLongOrNull() ?: (16L * 1024 * 1024 * 1024)

            val cpuProcess = ProcessBuilder("sysctl", "-n", "hw.ncpu").start()
            val cpuCount = String(cpuProcess.inputStream.readAllBytes()).trim().toIntOrNull() ?: 8

            // Convert bytes to GiB for easier consumption
            val memGib = (memBytes / (1024 * 1024 * 1024)).toInt()

            HardwareSpecs(memGib, cpuCount)
        } catch (e: Exception) {
            e.printStackTrace()
            HardwareSpecs(16, 8) // Fallback
        }
    }
}
