package com.colimator.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Tray
import com.colimator.app.service.ActiveProfileRepository
import com.colimator.app.service.ActiveProfileRepositoryImpl
import com.colimator.app.service.ColimaService
import com.colimator.app.service.ColimaServiceImpl
import com.colimator.app.service.DockerService
import com.colimator.app.service.DockerServiceImpl
import com.colimator.app.service.JvmShellExecutor
import com.colimator.app.viewmodel.ContainersViewModel
import com.colimator.app.viewmodel.DashboardViewModel
import com.colimator.app.viewmodel.ImagesViewModel
import com.colimator.app.viewmodel.OnboardingViewModel
import com.colimator.app.viewmodel.ProfilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Taskbar
import javax.imageio.ImageIO

import org.jetbrains.compose.resources.painterResource
import colimator.composeapp.generated.resources.Res
import colimator.composeapp.generated.resources.app_icon
import colimator.composeapp.generated.resources.tray_icon



fun main() = application {
    // Dependency Injection Root
    val shellExecutor = JvmShellExecutor()
    val colimaService = ColimaServiceImpl(shellExecutor)
    val dockerService = DockerServiceImpl(shellExecutor)
    
    // Shared profile repository
    val activeProfileRepository = ActiveProfileRepositoryImpl()
    
    // ViewModels with profile awareness
    val onboardingViewModel = OnboardingViewModel(colimaService, dockerService)
    val dashboardViewModel = DashboardViewModel(colimaService, activeProfileRepository)
    val containersViewModel = ContainersViewModel(dockerService, activeProfileRepository)
    val imagesViewModel = ImagesViewModel(dockerService, activeProfileRepository)
    val profilesViewModel = ProfilesViewModel(colimaService, activeProfileRepository)
    
    // Coroutine scope for tray actions
    val trayScope = CoroutineScope(Dispatchers.IO)
    
    // Window state - use visible property instead of conditional rendering
    var isWindowVisible by remember { mutableStateOf(true) }
    val windowState = remember { WindowState() }

    val appIcon = painterResource(Res.drawable.app_icon)
    val trayIcon = painterResource(Res.drawable.tray_icon)

    // Window icon is handled by the Window composable below and the native distribution config

    Tray(
        icon = trayIcon,
        menu = {
            Item(
                "Start Colima",
                onClick = {
                    trayScope.launch { 
                        colimaService.start(activeProfileRepository.activeProfile.value) 
                    }
                    dashboardViewModel.refreshStatus()
                }
            )
            Item(
                "Stop Colima",
                onClick = {
                    trayScope.launch { 
                        colimaService.stop(activeProfileRepository.activeProfile.value) 
                    }
                    dashboardViewModel.refreshStatus()
                }
            )
            Separator()
            Item(
                "Show Window",
                onClick = {
                    isWindowVisible = true
                    windowState.isMinimized = false
                    windowState.placement = WindowPlacement.Floating
                }
            )
            Separator()
            Item(
                "Quit",
                onClick = {
                    exitApplication()
                }
            )
        },
        tooltip = "Colimator",
        onAction = {
             isWindowVisible = !isWindowVisible
             if (isWindowVisible) {
                 windowState.isMinimized = false
                 windowState.placement = WindowPlacement.Floating
             }
        }
    )

    // Main window - always rendered, visibility controlled
    Window(
        onCloseRequest = {
            // Minimize to tray instead of exiting
             isWindowVisible = false
        },
        visible = isWindowVisible,
        state = windowState,
        title = "Colimator",
        icon = appIcon
    ) {
        App(
            onboardingViewModel = onboardingViewModel,
            dashboardViewModel = dashboardViewModel,
            containersViewModel = containersViewModel,
            imagesViewModel = imagesViewModel,
            profilesViewModel = profilesViewModel,
            activeProfileRepository = activeProfileRepository,
            onExit = ::exitApplication
        )
    }
}


