package com.colimator.app.service

import com.colimator.app.util.FakeShellExecutor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ColimaServiceTest {

    private val shell = FakeShellExecutor()
    private val service = ColimaServiceImpl(shell)

    @Test
    fun `getStatus returns Starting when json says so even if exit code is 0`() = runTest {
        val statusJson = """{"cpu":2,"memory":2147483648,"disk":10737418240,"arch":"aarch64","runtime":"docker","address":"127.0.0.1"}"""
        
        // This simulates the behavior where we rely on `colima list` to get better status
        // But first let's see if we can assert the bug.
        // The bug is: internal implementation ONLY looks at exit code.
        // So if I return exit code 0, it says Running.
        // I want it to say Starting if I somehow know it is starting.
        // But `colima status` doesn't give "Starting".
        // `colima list` DOES.
        
        // So I will Mock `colima list --json` to return a profile with status "Starting".
        // And I expect `getStatus` to use `listProfiles` (or similar) to find that out.
        
        // Mock `status` to return success (so strict exit code check would say Running)
        // But Mock `list` effectively too?
        // Actually, I am modifying the SERVICE. So I can dictate that `getStatus` MUST verify against `listProfiles` or parse output.
        // If I change `getStatus` to call `list` internally, I need to mock `list`.
        
        // Let's write the test assuming `getStatus("default")` should return `VmStatus.Starting`.
        // To support this, I must mock whatever command `getStatus` WILL use.
        // Ideally `getStatus` should use `colima list --json` filtered by name.
        
        val listJson = """{"name":"default","status":"Starting","arch":"aarch64","cpu":2,"memory":2147483648,"disk":64424509440,"runtime":"docker","address":""}"""
        
        // Current implementation uses `colima status`. I want it to change.
        // So I will mock `colima list` and expect it to be called.
        // If the implementation isn't changed yet, this test might fail because it calls `colima status` (which I won't mock, so it returns failure -> Stopped? Or I mock `status` to success -> Running).
        
        // Let's mock `status` to success (Running) AND `list` to Starting.
        // If bug exists -> returns Running.
        // If fixed -> returns Starting.
        
        shell.addResponse(
            "colima", 
            listOf("status", "--json", "--profile", "default"), 
            CommandResult(0, statusJson, "")
        )
        
        shell.addResponse(
            "colima", 
            listOf("list", "--json"), 
            CommandResult(0, listJson, "")
        )

        val status = service.getStatus("default")
        assertEquals(VmStatus.Starting, status)
    }

    @Test
    fun `listProfiles parses json correctly`() = runTest {
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
