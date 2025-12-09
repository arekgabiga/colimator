package com.colimator.app.viewmodel

import com.colimator.app.model.Profile
import com.colimator.app.service.FakeColimaService
import com.colimator.app.service.VmStatus
import com.colimator.app.util.FakeActiveProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var colimaService: FakeColimaService
    private lateinit var activeProfileRepository: FakeActiveProfileRepository
    private lateinit var viewModel: DashboardViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        colimaService = FakeColimaService()
        activeProfileRepository = FakeActiveProfileRepository()
        
        // Setup default state in fake service
        colimaService.setInstalled(true)
        colimaService.setProfiles(
            listOf(
                Profile("default", VmStatus.Stopped, isActive = false)
            )
        )
        
        // ViewModel starts listening in init block, so we instantiate it here
        viewModel = DashboardViewModel(colimaService, activeProfileRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // init starts refreshing status. With UnconfinedTestDispatcher it runs immediately 
        // until suspension. But refreshStatus calls delay implicitly? No, service doesn't have delay set yet.
        // So it runs to completion if no delay.
        
        val state = viewModel.state.value
        assertEquals("default", state.activeProfile)
        assertEquals(VmStatus.Stopped, state.vmStatus)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `startVm updates status to Starting then refreshes`() = runTest {
        colimaService.simulationDelay = 100 // Add delay to catch intermediate state
        
        viewModel.startVm()
        // With UnconfinedTestDispatcher, this runs until delay() in service.start()
        
        // Should show starting
        assertEquals(VmStatus.Starting, viewModel.state.value.vmStatus)
        assertTrue(viewModel.state.value.isLoading)
        
        testDispatcher.scheduler.advanceUntilIdle() // Finish
        
        // After completion
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(VmStatus.Running, viewModel.state.value.vmStatus)
    }

    @Test
    fun `stopVm updates status to Stopping then refreshes`() = runTest {
        // Setup as running first
        colimaService.setProfiles(listOf(Profile("default", VmStatus.Running, isActive = true)))
        // Ensure refreshStatus from init completes
        
        viewModel.refreshStatus()
        assertEquals(VmStatus.Running, viewModel.state.value.vmStatus)
        
        colimaService.simulationDelay = 100
        
        viewModel.stopVm()
        
        assertEquals(VmStatus.Stopping, viewModel.state.value.vmStatus)
        assertTrue(viewModel.state.value.isLoading)
        
        testDispatcher.scheduler.advanceUntilIdle() // Finish
        
        assertEquals(VmStatus.Stopped, viewModel.state.value.vmStatus)
        assertFalse(viewModel.state.value.isLoading)
    }
    
    @Test
    fun `deleteVm stops running vm then deletes it`() = runTest {
        colimaService.setProfiles(listOf(Profile("default", VmStatus.Running, isActive = true)))
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.deleteVm()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should be gone from service
        assertTrue(colimaService.listProfiles().isEmpty())
        
        // Repository should be cleared (active profile null)
        assertNull(activeProfileRepository.activeProfile.value)
    }
}
