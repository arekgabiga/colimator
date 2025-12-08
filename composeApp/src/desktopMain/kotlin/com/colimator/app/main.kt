package com.colimator.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.colimator.app.service.ColimaService
import com.colimator.app.service.DockerService
import com.colimator.app.service.JvmShellExecutor
import com.colimator.app.viewmodel.DashboardViewModel
import com.colimator.app.viewmodel.OnboardingViewModel

fun main() = application {
    // Dependency Injection Root
    val shellExecutor = JvmShellExecutor()
    val colimaService = ColimaService(shellExecutor)
    val dockerService = DockerService(shellExecutor)
    val onboardingViewModel = OnboardingViewModel(colimaService, dockerService)
    val dashboardViewModel = DashboardViewModel(colimaService)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Colimator",
    ) {
        App(
            onboardingViewModel = onboardingViewModel,
            dashboardViewModel = dashboardViewModel,
            onExit = ::exitApplication
        )
    }
}
