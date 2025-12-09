package com.colimator.app.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DockerServiceTest {
    
    private val mockExecutor = MockShellExecutor()
    private val service = DockerServiceImpl(mockExecutor)
    
    @Test
    fun `isInstalled returns true when docker version succeeds`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--version"),
            CommandResult(0, "Docker version 24.0.0", "")
        )
        
        assertTrue(service.isInstalled())
    }
    
    @Test
    fun `isInstalled returns false when docker not found`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--version"),
            CommandResult(127, "", "command not found: docker")
        )
        
        assertFalse(service.isInstalled())
    }
    
    @Test
    fun `listContainers returns empty list when no containers`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "ps", "-a", "--format", "json"),
            CommandResult(0, "", "")
        )
        
        val containers = service.listContainers()
        
        assertTrue(containers.isEmpty())
    }
    
    @Test
    fun `listContainers parses container JSON correctly`() = runBlocking {
        val containerJson = """{"ID":"abc123","Names":"nginx","Image":"nginx:latest","Status":"Up 2 hours","Ports":"80/tcp","State":"running"}"""
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "ps", "-a", "--format", "json"),
            CommandResult(0, containerJson, "")
        )
        
        val containers = service.listContainers()
        
        assertEquals(1, containers.size)
        assertEquals("abc123", containers[0].id)
        assertEquals("nginx", containers[0].names)
        assertEquals("nginx:latest", containers[0].image)
        assertTrue(containers[0].isRunning)
    }
    
    @Test
    fun `listContainers handles multiple containers`() = runBlocking {
        val json = """
            {"ID":"abc123","Names":"nginx","Image":"nginx:latest","Status":"Up","Ports":"80/tcp","State":"running"}
            {"ID":"def456","Names":"redis","Image":"redis:alpine","Status":"Exited","Ports":"","State":"exited"}
        """.trimIndent()
        
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "ps", "-a", "--format", "json"),
            CommandResult(0, json, "")
        )
        
        val containers = service.listContainers()
        
        assertEquals(2, containers.size)
        assertTrue(containers[0].isRunning)
        assertFalse(containers[1].isRunning)
    }
    
    @Test
    fun `listContainers uses profile-specific context`() = runBlocking {
        val containerJson = """{"ID":"dev123","Names":"dev-container","Image":"alpine","Status":"Up","Ports":"","State":"running"}"""
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima-dev", "ps", "-a", "--format", "json"),
            CommandResult(0, containerJson, "")
        )
        
        val containers = service.listContainers("dev")
        
        assertEquals(1, containers.size)
        assertEquals("dev123", containers[0].id)
    }
    
    @Test
    fun `startContainer calls docker start with id`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "start", "abc123"),
            CommandResult(0, "abc123", "")
        )
        
        val result = service.startContainer("abc123")
        
        assertTrue(result.isSuccess())
    }
    
    @Test
    fun `stopContainer calls docker stop with id`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "stop", "abc123"),
            CommandResult(0, "abc123", "")
        )
        
        val result = service.stopContainer("abc123")
        
        assertTrue(result.isSuccess())
    }
    
    @Test
    fun `removeContainer calls docker rm with id`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "rm", "abc123"),
            CommandResult(0, "abc123", "")
        )
        
        val result = service.removeContainer("abc123")
        
        assertTrue(result.isSuccess())
    }
    
    // --- Image tests ---
    
    @Test
    fun `listImages returns empty list when no images`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "images", "--format", "json"),
            CommandResult(0, "", "")
        )
        
        val images = service.listImages()
        
        assertTrue(images.isEmpty())
    }
    
    @Test
    fun `listImages parses image JSON correctly`() = runBlocking {
        val imageJson = """{"ID":"sha256:abc123","Repository":"nginx","Tag":"latest","Size":"142MB"}"""
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "images", "--format", "json"),
            CommandResult(0, imageJson, "")
        )
        
        val images = service.listImages()
        
        assertEquals(1, images.size)
        assertEquals("sha256:abc123", images[0].id)
        assertEquals("nginx", images[0].repository)
        assertEquals("latest", images[0].tag)
        assertEquals("142MB", images[0].size)
        assertEquals("nginx:latest", images[0].displayName)
        assertEquals(142_000_000L, images[0].sizeInBytes)
    }
    
    @Test
    fun `listImages handles multiple images`() = runBlocking {
        val json = """
            {"ID":"sha256:abc123","Repository":"nginx","Tag":"latest","Size":"1.19GB"}
            {"ID":"sha256:def456","Repository":"redis","Tag":"alpine","Size":"32MB"}
        """.trimIndent()
        
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima", "images", "--format", "json"),
            CommandResult(0, json, "")
        )
        
        val images = service.listImages()
        
        assertEquals(2, images.size)
        assertEquals("nginx:latest", images[0].displayName)
        assertEquals("redis:alpine", images[1].displayName)
        // Verify size parsing
        assertEquals(1_190_000_000L, images[0].sizeInBytes)
        assertEquals(32_000_000L, images[1].sizeInBytes)
    }
    
    @Test
    fun `listImages uses profile-specific context`() = runBlocking {
        val imageJson = """{"ID":"sha256:dev123","Repository":"alpine","Tag":"latest","Size":"5MB"}"""
        mockExecutor.mockResponse(
            "docker", 
            listOf("--context", "colima-dev", "images", "--format", "json"),
            CommandResult(0, imageJson, "")
        )
        
        val images = service.listImages("dev")
        
        assertEquals(1, images.size)
        assertEquals("sha256:dev123", images[0].id)
    }
}
