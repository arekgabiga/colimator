package com.colimator.app.viewmodel

import com.colimator.app.model.DockerImage
import com.colimator.app.model.DockerImageWithUsage
import com.colimator.app.service.DockerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sort field options for images list.
 */
enum class ImageSortField { NAME, SIZE }

/**
 * Sort direction.
 */
enum class SortDirection { ASC, DESC }

/**
 * State for the images list screen.
 */
data class ImagesState(
    val images: List<DockerImageWithUsage> = emptyList(),
    val sortField: ImageSortField = ImageSortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortVersion: Int = 0  // Incremented on sort change to trigger scroll reset
)

/**
 * ViewModel for managing Docker images list with sorting.
 */
class ImagesViewModel(private val dockerService: DockerService) : BaseViewModel() {
    
    private val _state = MutableStateFlow(ImagesState())
    val state: StateFlow<ImagesState> = _state.asStateFlow()
    
    init {
        refresh()
    }
    
    /**
     * Refresh the images list from Docker and determine usage status.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch images and containers in parallel
                val images = dockerService.listImages()
                val containers = dockerService.listContainers()
                
                // Get set of image names used by containers
                val usedImages = containers.map { it.image }.toSet()
                
                // Map images with usage status
                val imagesWithUsage = images.map { image ->
                    DockerImageWithUsage(
                        image = image,
                        isInUse = usedImages.any { used -> 
                            used == image.displayName || 
                            used.startsWith(image.repository + ":") ||
                            used == image.repository
                        }
                    )
                }
                
                val sorted = sortImages(imagesWithUsage, _state.value.sortField, _state.value.sortDirection)
                _state.update { it.copy(images = sorted, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load images", isLoading = false) }
            }
        }
    }
    
    /**
     * Set sort field. Toggles direction if same field, resets to ASC if different.
     */
    fun setSortField(field: ImageSortField) {
        _state.update { current ->
            val newDirection = if (current.sortField == field) {
                if (current.sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            } else {
                SortDirection.ASC
            }
            val sorted = sortImages(current.images, field, newDirection)
            current.copy(
                images = sorted, 
                sortField = field, 
                sortDirection = newDirection,
                sortVersion = current.sortVersion + 1
            )
        }
    }
    
    /**
     * Clear any displayed error.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun sortImages(
        images: List<DockerImageWithUsage>,
        field: ImageSortField,
        direction: SortDirection
    ): List<DockerImageWithUsage> {
        val comparator: Comparator<DockerImageWithUsage> = when (field) {
            ImageSortField.NAME -> compareBy { it.image.displayName.lowercase() }
            ImageSortField.SIZE -> compareBy { it.image.sizeInBytes }
        }
        return if (direction == SortDirection.ASC) {
            images.sortedWith(comparator)
        } else {
            images.sortedWith(comparator.reversed())
        }
    }
}
