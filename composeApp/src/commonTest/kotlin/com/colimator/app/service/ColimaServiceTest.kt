package com.colimator.app.service

import com.colimator.app.util.FakeShellExecutor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ColimaServiceTest {

    private val shell = FakeShellExecutor()
    private val service = ColimaServiceImpl(shell)

    @Test
    fun `getStatus returns Starting when json says so`() = runTest {
        // Mock `colima list --json` to return a profile with status "Starting".
        // This confirms that getStatus parses the JSON output rather than just checking exit code.
        
        val listJson = """{"name":"default","status":"Starting","arch":"aarch64","cpu":2,"memory":2147483648,"disk":64424509440,"runtime":"docker","address":""}"""
        
        // We mock both 'list' (if it uses that) and ensure 'status' command interaction is considered if relevant,
        // but existing implementation in ColimaServiceImpl.getStatus currently uses listProfiles().
        
        shell.addResponse(
            "colima", 
            listOf("list", "--json"), 
            CommandResult(0, listJson, "")
        )

        val status = service.getStatus("default")
        assertEquals(VmStatus.Starting, status)
    }

    @Test
    fun `getStatus returns Stopped when profile not found in list`() = runTest {
        val listJson = """{"name":"other","status":"Running","arch":"aarch64","cpu":2,"memory":2147483648,"disk":64424509440,"runtime":"docker","address":""}"""
        
        shell.addResponse(
            "colima", 
            listOf("list", "--json"), 
            CommandResult(0, listJson, "")
        )

        val status = service.getStatus("default")
        assertEquals(VmStatus.Stopped, status)
    }

    @Test
    fun `listProfiles parses json correctly with mixed statuses`() = runTest {
        val jsonOutput = """
            {"name":"default","status":"Running","arch":"aarch64","cpu":2,"memory":2147483648,"disk":64424509440,"runtime":"docker","address":"192.168.106.2"}
            {"name":"test-profile","status":"Stopped","arch":"aarch64","cpu":2,"memory":2147483648,"disk":64424509440,"runtime":"docker","address":""}
        """.trimIndent()

        shell.addResponse(
            "colima",
            listOf("list", "--json"),
            CommandResult(0, jsonOutput, "")
        )

        val profiles = service.listProfiles()
        assertEquals(2, profiles.size)
        assertEquals("default", profiles[0].name)
        assertEquals(VmStatus.Running, profiles[0].status)
        assertEquals("test-profile", profiles[1].name)
        assertEquals(VmStatus.Stopped, profiles[1].status)
    }
}
