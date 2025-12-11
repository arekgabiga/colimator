package com.colimator.app.viewmodel

import com.colimator.app.model.Container
import com.colimator.app.model.ContainerInspection
import com.colimator.app.model.DockerImage
import com.colimator.app.service.ActiveProfileRepository
import com.colimator.app.service.CommandResult
import com.colimator.app.service.DockerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContainersViewModelTest {

    private lateinit var dockerService: FakeDockerService
    private lateinit var activeProfileRepository: FakeActiveProfileRepository
    private lateinit var viewModel: ContainersViewModel
    
    // Test Containers
    private val container1 = Container(
        id = "1",
        image = "nginx:latest", 
        names = "web-server",
        status = "Up 2 hours",
        ports = "80/tcp->8080/tcp",
        state = "running"
    )
    
    private val container2 = Container(
        id = "2",
        image = "postgres:14", 
        names = "db",
        status = "Exited (0)",
        ports = "",
        state = "exited"
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        dockerService = FakeDockerService()
        activeProfileRepository = FakeActiveProfileRepository()
        viewModel = ContainersViewModel(dockerService, activeProfileRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialRefresh() = runTest {
        dockerService.containers = listOf(container1, container2)
        
        viewModel.refresh()
        advanceUntilIdle()
        
        assertEquals(2, viewModel.state.value.containers.size)
        assertEquals("db", viewModel.state.value.containers[0].displayName) // Sorted by name ASC by default
        assertEquals("web-server", viewModel.state.value.containers[1].displayName)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun testSorting() = runTest {
        dockerService.containers = listOf(container1, container2)
        viewModel.refresh()
        advanceUntilIdle()
        
        // Initial: Name ASC -> db, web-server
        assertEquals("db", viewModel.state.value.containers[0].displayName)
        assertEquals("web-server", viewModel.state.value.containers[1].displayName)
        
        // Toggle (Name DESC)
        viewModel.setSortField(ContainerSortField.NAME)
        assertEquals("web-server", viewModel.state.value.containers[0].displayName)
        assertEquals("db", viewModel.state.value.containers[1].displayName)
        
        // Sort by Status (Running first) -> web-server (Up), db (Exited)
        viewModel.setSortField(ContainerSortField.STATUS)
        assertEquals("web-server", viewModel.state.value.containers[0].displayName)
        
        // Toggle Status (Running last / DESC) -> db, web-server
        viewModel.setSortField(ContainerSortField.STATUS)
        assertEquals("db", viewModel.state.value.containers[0].displayName)
    }

    @Test
    fun testStartContainer() = runTest {
        viewModel.startContainer("2")
        advanceUntilIdle()
        
        assertTrue(dockerService.startCalled)
        assertEquals("2", dockerService.lastContainerId)
        // Should refresh after success
        // In this fake, refresh just returns the static list, but we verified the call.
    }
    
    @Test
    fun testStartContainerError() = runTest {
        dockerService.shouldFail = true
        viewModel.startContainer("2")
        advanceUntilIdle()
        
        assertEquals("Simulated failure", viewModel.state.value.error)
    }

    @Test
    fun testProfileSwitch() = runTest {
        // Initial state
        dockerService.containers = listOf(container1)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.containers.size)

        // Switch profile
        dockerService.containers = listOf(container1, container2) // New profile has 2
        activeProfileRepository.emit("new-profile")
        advanceUntilIdle()
        
        assertEquals("new-profile", viewModel.state.value.activeProfile)
        assertEquals(2, viewModel.state.value.containers.size)
    }

    @Test
    fun testOnScreenVisibleRefresh() = runTest {
        activeProfileRepository.hasChanged = true
        
        viewModel.onScreenVisible()
        advanceUntilIdle()
        
        assertTrue(activeProfileRepository.ackCalled)
    }
}

// Fakes
class FakeDockerService : DockerService {
    var containers = emptyList<Container>()
    var shouldFail = false
    var startCalled = false
    var stopCalled = false
    var removeCalled = false
    var lastContainerId = ""

    override suspend fun isInstalled(): Boolean = true

    override suspend fun listContainers(profileName: String?): List<Container> {
        if (shouldFail) throw RuntimeException("Simulated failure")
        return containers
    }

    override suspend fun listImages(profileName: String?): List<DockerImage> {
        return emptyList()
    }

    override suspend fun inspectContainer(id: String, profileName: String?): ContainerInspection? {
        // Not used in this specific VM test
        return null
    }

    override suspend fun startContainer(id: String, profileName: String?): CommandResult {
        startCalled = true
        lastContainerId = id
        return if (shouldFail) CommandResult(1, "", "Simulated failure") else CommandResult(0, "", "")
    }

    override suspend fun stopContainer(id: String, profileName: String?): CommandResult {
        stopCalled = true
        lastContainerId = id
        return if (shouldFail) CommandResult(1, "", "Simulated failure") else CommandResult(0, "", "")
    }

    override suspend fun removeContainer(id: String, profileName: String?): CommandResult {
        removeCalled = true
        lastContainerId = id
        return if (shouldFail) CommandResult(1, "", "Simulated failure") else CommandResult(0, "", "")
    }

    override suspend fun removeImage(imageId: String, profileName: String?): CommandResult {
        return CommandResult(0, "", "")
    }
}

class FakeActiveProfileRepository : ActiveProfileRepository {
    private val _activeProfile = MutableStateFlow<String?>("default")
    override val activeProfile: StateFlow<String?> = _activeProfile.asStateFlow()
    
    var hasChanged = false
    var ackCalled = false

    suspend fun emit(value: String) {
        _activeProfile.emit(value)
    }

    override fun setActiveProfile(profileName: String?) {
        _activeProfile.value = profileName
    }

    override fun hasProfileChanged(): Boolean = hasChanged

    override fun acknowledgeProfileChange() {
        ackCalled = true
        hasChanged = false
    }

    override fun getActiveProfileDisplayName(): String = _activeProfile.value ?: "default"
}
