package com.colimator.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.colimator.app.service.ColimaService
import com.colimator.app.service.JvmShellExecutor
import com.colimator.app.viewmodel.DashboardViewModel

fun main() = application {
    // Dependency Injection Root
    val shellExecutor = JvmShellExecutor()
    val colimaService = ColimaService(shellExecutor)
    val dashboardViewModel = DashboardViewModel(colimaService)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Colimator",
    ) {
        App(dashboardViewModel)
    }
}
