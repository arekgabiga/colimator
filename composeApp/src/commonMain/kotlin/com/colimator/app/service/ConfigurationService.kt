package com.colimator.app.service

import com.colimator.app.domain.ColimaConfig
import com.colimator.app.domain.HardwareSpecs

expect object ConfigurationService {
    fun loadConfig(profileName: String): ColimaConfig
    fun saveConfig(profileName: String, config: ColimaConfig)
    fun getHostHardwareSpecs(): HardwareSpecs
}
