package com.colimator.app.viewmodel

import com.colimator.app.model.Container
import com.colimator.app.model.DockerImage
import com.colimator.app.service.FakeDockerService
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImagesViewModelTest {

    private lateinit var dockerService: FakeDockerService
    private lateinit var activeProfileRepository: FakeActiveProfileRepository
    private lateinit var viewModel: ImagesViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        dockerService = FakeDockerService()
        activeProfileRepository = FakeActiveProfileRepository()
        
        viewModel = ImagesViewModel(dockerService, activeProfileRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `refresh loads images and marks usage correctly`() = runTest {
        val image1 = DockerImage("1", "repo/image1", "latest", "1000B")
        val image2 = DockerImage("2", "repo/image2", "v1", "2000B")
        
        // Image 1 is used by a container
        val container = Container("c1", "test-container", "repo/image1:latest", "Exited", "", "exited")
        
        dockerService.setImages(listOf(image1, image2))
        dockerService.setContainers(listOf(container))
        
        viewModel.refresh()
        
        val state = viewModel.state.value
        assertEquals(2, state.images.size)
        
        // Sort defaults to NAME ASC
        val img1 = state.images.find { it.image.id == "1" }
        assertNotNull(img1)
        assertTrue(img1.isInUse)
        
        val img2 = state.images.find { it.image.id == "2" }
        assertNotNull(img2)
        assertFalse(img2.isInUse)
    }
    
    @Test
    fun `sorting works correctly`() = runTest {
        val imageA = DockerImage("1", "a-image", "latest", "2000B")
        val imageB = DockerImage("2", "b-image", "latest", "1000B")
        
        dockerService.setImages(listOf(imageA, imageB))
        viewModel.refresh()
        
        // Default: NAME ASC -> a, b
        assertEquals("a-image", viewModel.state.value.images[0].image.repository)
        assertEquals("b-image", viewModel.state.value.images[1].image.repository)
        
        // Change to SIZE ASC -> b (1000), a (2000)
        viewModel.setSortField(ImageSortField.SIZE)
        assertEquals(ImageSortField.SIZE, viewModel.state.value.sortField)
        assertEquals(SortDirection.ASC, viewModel.state.value.sortDirection)
        assertEquals("b-image", viewModel.state.value.images[0].image.repository)
        
        // Toggle (SIZE DESC) -> a (2000), b (1000)
        viewModel.setSortField(ImageSortField.SIZE)
        assertEquals(SortDirection.DESC, viewModel.state.value.sortDirection)
        assertEquals("a-image", viewModel.state.value.images[0].image.repository)
    }
    
    @Test
    fun `delete flow works`() = runTest {
        val image = DockerImage("1", "repo/image1", "latest", "1000B")
        dockerService.setImages(listOf(image))
        viewModel.refresh()
        
        val imageWithUsage = viewModel.state.value.images.first()
        
        // Request delete
        viewModel.requestDelete(imageWithUsage)
        assertEquals(imageWithUsage, viewModel.state.value.imagePendingDelete)
        
        // Confirm delete
        viewModel.confirmDelete()
        
        // Should be removed
        assertNull(viewModel.state.value.imagePendingDelete)
        assertFalse(viewModel.state.value.images.any { it.image.id == "1" })
        assertTrue(dockerService.listImages("default").isEmpty())
    }
}
