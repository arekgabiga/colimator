package com.colimator.app.service

import com.colimator.app.domain.ColimaConfig
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationServiceTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = java.nio.file.Files.createTempDirectory("colima-test").toFile()
        ConfigurationService.colimaDir = tempDir
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `loadConfig returns default if file does not exist`() {
        val config = ConfigurationService.loadConfig("non-existent")
        assertEquals(2, config.cpu)
        assertEquals(2, config.memory)
        assertEquals(60, config.disk)
    }

    @Test
    fun `loadConfig parses existing yaml correctly`() {
        val profileDir = File(tempDir, "default").apply { mkdirs() }
        val yamlContent = """
            cpu: 4
            memory: 8
            disk: 100
            kubernetes:
              enabled: true
            runtime: containerd
        """.trimIndent()
        File(profileDir, "colima.yaml").writeText(yamlContent)

        val config = ConfigurationService.loadConfig("default")
        assertEquals(4, config.cpu)
        assertEquals(8, config.memory)
        assertEquals(100, config.disk)
        assertTrue(config.kubernetes)
        assertEquals("containerd", config.runtime)
    }

    @Test
    fun `saveConfig writes yaml correctly`() {
        val profileDir = File(tempDir, "test-save").apply { mkdirs() }
        val initialConfig = ColimaConfig(cpu = 6, memory = 12, disk = 80, kubernetes = true)
        
        ConfigurationService.saveConfig("test-save", initialConfig)
        
        val configFile = File(profileDir, "colima.yaml")
        assertTrue(configFile.exists())
        
        val content = configFile.readText()
        assertTrue(content.contains("cpu: 6"))
        assertTrue(content.contains("memory: 12"))
        
        // Verify round trip
        val loaded = ConfigurationService.loadConfig("test-save")
        assertEquals(6, loaded.cpu)
        assertEquals(12, loaded.memory)
        assertEquals(80, loaded.disk)
        assertTrue(loaded.kubernetes)
    }
}
