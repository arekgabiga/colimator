package com.colimator.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Docker image from 'docker images --format json'.
 */
@Serializable
data class DockerImage(
    @SerialName("ID") val id: String,
    @SerialName("Repository") val repository: String,
    @SerialName("Tag") val tag: String,
    @SerialName("Size") val size: String   // Human-readable (e.g., "1.19GB")
) {
    /**
     * Display name combining repository and tag.
     */
    val displayName: String
        get() = if (tag.isNotBlank() && tag != "<none>") "$repository:$tag" else repository
    
    /**
     * Short ID (first 12 characters).
     */
    val shortId: String
        get() = id.take(12)
    
    /**
     * Parse size string (e.g., "1.19GB", "142MB", "5.2kB") to bytes for sorting.
     */
    val sizeInBytes: Long
        get() {
            val regex = """([\d.]+)\s*(B|kB|KB|MB|GB|TB)?""".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(size) ?: return 0L
            val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
            val unit = match.groupValues[2].uppercase()
            val multiplier = when (unit) {
                "TB" -> 1_000_000_000_000L
                "GB" -> 1_000_000_000L
                "MB" -> 1_000_000L
                "KB", "KB" -> 1_000L
                else -> 1L
            }
            return (value * multiplier).toLong()
        }
}

/**
 * UI model combining Docker image with its usage status.
 */
data class DockerImageWithUsage(
    val image: DockerImage,
    val isInUse: Boolean  // true if any container uses this image
)
