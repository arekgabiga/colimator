package com.colimator.app.domain

data class ColimaConfig(
    val cpu: Int = 2,
    val memory: Int = 2,
    val disk: Int = 60,
    val kubernetes: Boolean = false,
    val runtime: String = "docker" // Read-only mostly for now, but good to have
)

data class HardwareSpecs(val memoryGib: Int, val cpuCount: Int)
