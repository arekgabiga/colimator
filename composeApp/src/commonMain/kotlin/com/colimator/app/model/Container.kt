package com.colimator.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Docker container from 'docker ps -a --format json'.
 */
@Serializable
data class Container(
    @SerialName("ID") val id: String,
    @SerialName("Names") val names: String,
    @SerialName("Image") val image: String,
    @SerialName("Status") val status: String,
    @SerialName("Ports") val ports: String = "",
    @SerialName("State") val state: String  // "running", "exited", "created", etc.
) {
    /**
     * Whether the container is currently running.
     */
    val isRunning: Boolean
        get() = state.lowercase() == "running"
    
    /**
     * Display name (first name if multiple).
     */
    val displayName: String
        get() = names.split(",").firstOrNull()?.trim() ?: names
}
