package com.colimator.app.viewmodel

import com.colimator.app.service.ColimaService
import com.colimator.app.service.DockerService
import com.colimator.app.service.CommandResult
import com.colimator.app.model.Container
import com.colimator.app.model.DockerImage
import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig
import com.colimator.app.service.ColimaConfig
import com.colimator.app.model.ContainerInspection
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var colimaService: FakeOnboardingColimaService
    private lateinit var dockerService: FakeOnboardingDockerService
    private lateinit var viewModel: OnboardingViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        colimaService = FakeOnboardingColimaService()
        dockerService = FakeOnboardingDockerService()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAllInstalled() = runTest {
        colimaService.isInstalled = true
        dockerService.isInstalled = true
        
        viewModel = OnboardingViewModel(colimaService, dockerService)
        advanceUntilIdle() // Init calls checkDependencies

        assertTrue(viewModel.state.value is OnboardingState.Ready)
    }

    @Test
    fun testColimaMissing() = runTest {
        colimaService.isInstalled = false
        dockerService.isInstalled = true
        
        viewModel = OnboardingViewModel(colimaService, dockerService)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is OnboardingState.Missing)
        assertEquals(false, state.colimaInstalled)
        assertEquals(true, state.dockerInstalled)
        assertTrue(state.message.contains("Colima"))
    }
    
    @Test
    fun testBothMissing() = runTest {
        colimaService.isInstalled = false
        dockerService.isInstalled = false
        
        viewModel = OnboardingViewModel(colimaService, dockerService)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is OnboardingState.Missing)
        assertEquals(false, state.colimaInstalled)
        assertEquals(false, state.dockerInstalled)
        assertTrue(state.message.contains("Colima and Docker"))
    }
}

// Minimal Fakes for Onboarding testing
class FakeOnboardingColimaService : ColimaService {
    var isInstalled = false
    override suspend fun isInstalled(): Boolean = isInstalled
    override suspend fun listProfiles(): List<Profile> = emptyList()
    override suspend fun getStatus(profileName: String?): VmStatus = VmStatus.Unknown
    override suspend fun getConfig(profileName: String?): ColimaConfig? = null
    override suspend fun start(profileName: String?, config: ProfileCreateConfig?): CommandResult = CommandResult(0, "", "")
    override suspend fun stop(profileName: String?): CommandResult = CommandResult(0, "", "")
    override suspend fun delete(profileName: String): CommandResult = CommandResult(0, "", "")
}

class FakeOnboardingDockerService : DockerService {
    var isInstalled = false
    override suspend fun isInstalled(): Boolean = isInstalled
    override suspend fun listContainers(profileName: String?): List<Container> = emptyList()
    override suspend fun listImages(profileName: String?): List<DockerImage> = emptyList()
    override suspend fun inspectContainer(id: String, profileName: String?): ContainerInspection? = null
    override suspend fun startContainer(id: String, profileName: String?): CommandResult = CommandResult(0, "", "")
    override suspend fun stopContainer(id: String, profileName: String?): CommandResult = CommandResult(0, "", "")
    override suspend fun removeContainer(id: String, profileName: String?): CommandResult = CommandResult(0, "", "")
    override suspend fun removeImage(imageId: String, profileName: String?): CommandResult = CommandResult(0, "", "")
}
