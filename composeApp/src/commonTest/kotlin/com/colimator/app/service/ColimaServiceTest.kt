package com.colimator.app.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColimaServiceTest {
    
    private val mockExecutor = MockShellExecutor()
    private val service = ColimaService(mockExecutor)
    
    @Test
    fun `isInstalled returns true when colima version succeeds`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("--version"),
            CommandResult(0, "colima version 0.6.0", "")
        )
        
        assertTrue(service.isInstalled())
    }
    
    @Test
    fun `isInstalled returns false when colima not found`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("--version"),
            CommandResult(127, "", "command not found: colima")
        )
        
        assertFalse(service.isInstalled())
    }
    
    @Test
    fun `getStatus returns Running when colima reports running`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("status", "--output", "json"),
            CommandResult(0, """{"running":true}""", "")
        )
        
        assertEquals(VmStatus.Running, service.getStatus())
    }
    
    @Test
    fun `getStatus returns Stopped when colima reports not running`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("status", "--output", "json"),
            CommandResult(0, """{"running":false}""", "")
        )
        
        assertEquals(VmStatus.Stopped, service.getStatus())
    }
    
    @Test
    fun `getStatus returns Stopped when colima status fails`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("status", "--output", "json"),
            CommandResult(1, "", "colima is not running")
        )
        
        assertEquals(VmStatus.Stopped, service.getStatus())
    }
    
    @Test
    fun `start calls colima start`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("start"),
            CommandResult(0, "started", "")
        )
        
        val result = service.start()
        
        assertTrue(result.isSuccess())
        assertTrue(mockExecutor.getCalls().any { it.second == listOf("start") })
    }
    
    @Test
    fun `stop calls colima stop`() = runBlocking {
        mockExecutor.mockResponse(
            "colima", 
            listOf("stop"),
            CommandResult(0, "stopped", "")
        )
        
        val result = service.stop()
        
        assertTrue(result.isSuccess())
        assertTrue(mockExecutor.getCalls().any { it.second == listOf("stop") })
    }
}
