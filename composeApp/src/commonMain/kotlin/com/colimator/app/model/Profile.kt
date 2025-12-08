package com.colimator.app.model

import com.colimator.app.service.VmStatus

/**
 * Represents a Colima profile (VM instance).
 */
data class Profile(
    val name: String,
    val status: VmStatus,
    val cpuCores: Int = 0,
    val memoryBytes: Long = 0,
    val diskBytes: Long = 0,
    val isActive: Boolean = false
) {
    /** Memory in gigabytes */
    val memoryGb: Double get() = memoryBytes / 1_073_741_824.0
    
    /** Disk in gigabytes */
    val diskGb: Double get() = diskBytes / 1_073_741_824.0
    
    /** Display name for UI */
    val displayName: String get() = if (name == "default") "default" else name
}

/**
 * Configuration for creating a new Colima profile.
 * Defaults match Colima's built-in defaults.
 */
data class ProfileCreateConfig(
    val name: String,
    val cpuCores: Int = 2,        // Colima default: 2
    val memoryGb: Int = 2,        // Colima default: 2 GiB
    val diskGb: Int = 60,         // Colima default: 60 GiB
    val kubernetes: Boolean = false,
    val vmType: VmType = VmType.VZ,
    val mountType: MountType = MountType.VIRTIOFS
)

/**
 * VM virtualization type.
 */
enum class VmType(val cliValue: String) {
    VZ("vz"),       // macOS Virtualization.framework (faster, macOS 13+)
    QEMU("qemu")    // QEMU (more compatible)
}

/**
 * Volume mount type for sharing files between host and VM.
 */
enum class MountType(val cliValue: String) {
    VIRTIOFS("virtiofs"),  // Best performance (requires vz)
    SSHFS("sshfs"),        // Works with both vz and qemu
    P9("9p")               // Legacy option
}
