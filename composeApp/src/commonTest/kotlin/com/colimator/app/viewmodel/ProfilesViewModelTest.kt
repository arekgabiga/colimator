package com.colimator.app.viewmodel

import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig
import com.colimator.app.service.ColimaConfig
import com.colimator.app.service.ColimaService
import com.colimator.app.service.CommandResult
import com.colimator.app.service.VmStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class ProfilesViewModelTest {

    private lateinit var colimaService: FakeColimaService
    private lateinit var activeProfileRepository: FakeActiveProfileRepository
    private lateinit var viewModel: ProfilesViewModel

    private val profile1 = Profile(
        name = "default", 
        status = VmStatus.Running, 
        cpuCores = 2, 
        memoryBytes = 4L * 1024 * 1024 * 1024, 
        diskBytes = 60L * 1024 * 1024 * 1024,
        isActive = true
    )
    
    private val profile2 = Profile(
        name = "custom", 
        status = VmStatus.Stopped, 
        cpuCores = 4, 
        memoryBytes = 8L * 1024 * 1024 * 1024, 
        diskBytes = 100L * 1024 * 1024 * 1024, 
        isActive = false
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        colimaService = FakeColimaService()
        activeProfileRepository = FakeActiveProfileRepository()
        viewModel = ProfilesViewModel(colimaService, activeProfileRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialRefresh() = runTest {
        colimaService.profiles = listOf(profile1, profile2)
        viewModel.refreshProfiles()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.profiles.size)
        assertEquals("default", viewModel.state.value.profiles[0].name)
    }

    @Test
    fun testCreateProfile() = runTest {
        viewModel.showCreateDialog()
        assertTrue(viewModel.state.value.showCreateDialog)

        val config = viewModel.state.value.createConfig.copy(name = "new-profile")
        viewModel.updateCreateConfig(config)
        
        viewModel.createProfile()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showCreateDialog)
        assertTrue(colimaService.startCalled)
        assertEquals("new-profile", colimaService.lastProfileName)
    }

    @Test
    fun testCreateProfileValidationError() = runTest {
        viewModel.showCreateDialog()
        // Empty name
        viewModel.createProfile()
        advanceUntilIdle()

        assertEquals("Profile name is required", viewModel.state.value.error)
        assertFalse(colimaService.startCalled)
    }

    @Test
    fun testStartProfile() = runTest {
        viewModel.startProfile(profile2)
        advanceUntilIdle()

        assertTrue(colimaService.startCalled)
        assertEquals("custom", colimaService.lastProfileName)
    }

    @Test
    fun testStopProfile() = runTest {
        viewModel.stopProfile(profile1)
        advanceUntilIdle()

        assertTrue(colimaService.stopCalled)
        assertEquals("default", colimaService.lastProfileName)
    }

    @Test
    fun testDeleteProfile() = runTest {
        viewModel.confirmDeleteProfile(profile2)
        assertEquals(profile2, viewModel.state.value.profileToDelete)

        viewModel.deleteProfile()
        advanceUntilIdle()

        assertTrue(colimaService.deleteCalled)
        assertEquals("custom", colimaService.lastProfileName)
        assertNull(viewModel.state.value.profileToDelete)
    }

    @Test
    fun testDeleteRunningProfileStopsFirst() = runTest {
        viewModel.confirmDeleteProfile(profile1) // Running
        viewModel.deleteProfile()
        advanceUntilIdle()

        assertTrue(colimaService.stopCalled)
        assertTrue(colimaService.deleteCalled)
    }

    @Test
    fun testSwitchToProfile() = runTest {
        // Switch to custom
        viewModel.switchToProfile(profile2)
        advanceUntilIdle()
        
        assertEquals("custom", activeProfileRepository.activeProfile.value)
        // Since profile2 was stopped, it should try to start it
        assertTrue(colimaService.startCalled)
    }
    
    @Test
    fun testSwitchToDefaultProfile() = runTest {
        // Switch to default
        viewModel.switchToProfile(profile1)
        advanceUntilIdle()
        
        assertEquals(null, activeProfileRepository.activeProfile.value) // default maps to null in repo for CLI
    }
}

class FakeColimaService : ColimaService {
    var profiles = emptyList<Profile>()
    var startCalled = false
    var stopCalled = false
    var deleteCalled = false
    var lastProfileName: String? = ""
    var shouldFail = false

    override suspend fun isInstalled(): Boolean = true

    override suspend fun listProfiles(): List<Profile> {
        if (shouldFail) throw RuntimeException("Simulated error")
        return profiles
    }

    override suspend fun getStatus(profileName: String?): VmStatus {
        return VmStatus.Stopped
    }
    
    override suspend fun getConfig(profileName: String?): ColimaConfig? {
        return null
    }

    override suspend fun start(profileName: String?, config: ProfileCreateConfig?): CommandResult {
        startCalled = true
        lastProfileName = profileName
        return if (shouldFail) CommandResult(1, "", "Simulated error") else CommandResult(0, "", "")
    }

    override suspend fun stop(profileName: String?): CommandResult {
        stopCalled = true
        lastProfileName = profileName
        return if (shouldFail) CommandResult(1, "", "Simulated error") else CommandResult(0, "", "")
    }

    override suspend fun delete(profileName: String): CommandResult {
        deleteCalled = true
        lastProfileName = profileName
        return if (shouldFail) CommandResult(1, "", "Simulated error") else CommandResult(0, "", "")
    }
}
