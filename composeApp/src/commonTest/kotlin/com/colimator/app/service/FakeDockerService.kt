package com.colimator.app.service

import com.colimator.app.model.Container
import com.colimator.app.model.DockerImage

/**
 * Fake implementation of [DockerService] for testing.
 */
class FakeDockerService : DockerService {
    
    // In-memory state
    private val _containers = mutableListOf<Container>()
    private val _images = mutableListOf<DockerImage>()
    private var _isInstalled = true
    
    // Test configuration
    var removeImageResult: CommandResult = CommandResult(0, "", "")
    
    fun setContainers(containers: List<Container>) {
        _containers.clear()
        _containers.addAll(containers)
    }
    
    fun setImages(images: List<DockerImage>) {
        _images.clear()
        _images.addAll(images)
    }

    override suspend fun isInstalled(): Boolean = _isInstalled

    override suspend fun listContainers(profileName: String?): List<Container> {
        return _containers
    }

    override suspend fun listImages(profileName: String?): List<DockerImage> {
        return _images
    }

    override suspend fun startContainer(id: String, profileName: String?): CommandResult {
        // Simple mock implementation
        return CommandResult(0, "", "")
    }

    override suspend fun stopContainer(id: String, profileName: String?): CommandResult {
        return CommandResult(0, "", "")
    }

    override suspend fun removeContainer(id: String, profileName: String?): CommandResult {
        _containers.removeIf { it.id == id }
        return CommandResult(0, "", "")
    }

    override suspend fun removeImage(imageId: String, profileName: String?): CommandResult {
        if (!removeImageResult.isSuccess()) return removeImageResult
        
        _images.removeIf { it.id == imageId }
        return removeImageResult
    }
}
