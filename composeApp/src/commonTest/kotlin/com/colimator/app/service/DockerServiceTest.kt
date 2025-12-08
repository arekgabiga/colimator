package com.colimator.app.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DockerServiceTest {
    
    private val mockExecutor = MockShellExecutor()
    private val service = DockerService(mockExecutor)
    
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
            listOf("ps", "-a", "--format", "json"),
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
            listOf("ps", "-a", "--format", "json"),
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
            listOf("ps", "-a", "--format", "json"),
            CommandResult(0, json, "")
        )
        
        val containers = service.listContainers()
        
        assertEquals(2, containers.size)
        assertTrue(containers[0].isRunning)
        assertFalse(containers[1].isRunning)
    }
    
    @Test
    fun `startContainer calls docker start with id`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("start", "abc123"),
            CommandResult(0, "abc123", "")
        )
        
        val result = service.startContainer("abc123")
        
        assertTrue(result.isSuccess())
    }
    
    @Test
    fun `stopContainer calls docker stop with id`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("stop", "abc123"),
            CommandResult(0, "abc123", "")
        )
        
        val result = service.stopContainer("abc123")
        
        assertTrue(result.isSuccess())
    }
    
    @Test
    fun `removeContainer calls docker rm with id`() = runBlocking {
        mockExecutor.mockResponse(
            "docker", 
            listOf("rm", "abc123"),
            CommandResult(0, "abc123", "")
        )
        
        val result = service.removeContainer("abc123")
        
        assertTrue(result.isSuccess())
    }
}
