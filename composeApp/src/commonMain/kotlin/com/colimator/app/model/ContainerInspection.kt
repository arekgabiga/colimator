package com.colimator.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerInspection(
    @SerialName("Id") val id: String,
    @SerialName("Created") val created: String,
    @SerialName("Path") val path: String,
    @SerialName("Args") val args: List<String>,
    @SerialName("State") val state: ContainerState,
    @SerialName("Image") val image: String,
    @SerialName("NetworkSettings") val networkSettings: NetworkSettings,
    @SerialName("Mounts") val mounts: List<Mount> = emptyList(),
    @SerialName("Config") val config: ContainerConfig
)

@Serializable
data class ContainerState(
    @SerialName("Status") val status: String,
    @SerialName("Running") val running: Boolean,
    @SerialName("Paused") val paused: Boolean,
    @SerialName("Restarting") val restarting: Boolean,
    @SerialName("OOMKilled") val oomKilled: Boolean,
    @SerialName("Dead") val dead: Boolean,
    @SerialName("Pid") val pid: Int,
    @SerialName("ExitCode") val exitCode: Int,
    @SerialName("Error") val error: String,
    @SerialName("StartedAt") val startedAt: String,
    @SerialName("FinishedAt") val finishedAt: String
)

@Serializable
data class NetworkSettings(
    @SerialName("IPAddress") val ipAddress: String,
    @SerialName("Gateway") val gateway: String,
    @SerialName("Ports") val ports: Map<String, List<PortBinding>?> = emptyMap(),
    @SerialName("Networks") val networks: Map<String, NetworkDetail> = emptyMap()
)

@Serializable
data class PortBinding(
    @SerialName("HostIp") val hostIp: String,
    @SerialName("HostPort") val hostPort: String
)

@Serializable
data class NetworkDetail(
    @SerialName("IPAddress") val ipAddress: String,
    @SerialName("Gateway") val gateway: String,
    @SerialName("MacAddress") val macAddress: String
)

@Serializable
data class Mount(
    @SerialName("Type") val type: String,
    @SerialName("Source") val source: String,
    @SerialName("Destination") val destination: String,
    @SerialName("Mode") val mode: String,
    @SerialName("RW") val rw: Boolean
)

@Serializable
data class ContainerConfig(
    @SerialName("Env") val env: List<String> = emptyList(),
    @SerialName("Image") val image: String,
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null
)
