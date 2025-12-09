package com.colimator.app.viewmodel

import com.colimator.app.service.VmStatus
import com.colimator.app.util.FakeActiveProfileRepository
import com.colimator.app.util.FakeColimaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    
    private lateinit var colimaService: FakeColimaService
    private lateinit var repository: FakeActiveProfileRepository
    private lateinit var viewModel: DashboardViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        colimaService = FakeColimaService()
        repository = FakeActiveProfileRepository()
        viewModel = DashboardViewModel(colimaService, repository)
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.state.value
        // Initial dashboard state is Unknown vmStatus, etc.
        assertEquals(VmStatus.Unknown, state.vmStatus)
    }
    
    @Test
    fun `refreshStatus calls service and updates state`() = runTest {
        colimaService.vmStatus = VmStatus.Running
        viewModel.refreshStatus()
        testDispatcher.scheduler.advanceUntilIdle() // Process coroutines
        
        val state = viewModel.state.value
        assertEquals(VmStatus.Running, state.vmStatus)
    }
    
    @Test
    fun `startVm calls service start`() = runTest {
        // Must ensure we have a profile or default
        // The mock service start will log "start null" if no profile set
        
        viewModel.startVm()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(colimaService.executedCommands.contains("start null")) 
        
        // Mock success -> should trigger refresh and potentially status change
        // FakeService returns success for start.
    }
}
