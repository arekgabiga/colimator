package com.colimator.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.colimator.app.service.ColimaService
import com.colimator.app.service.DockerService
import com.colimator.app.service.JvmShellExecutor
import com.colimator.app.viewmodel.ContainersViewModel
import com.colimator.app.viewmodel.DashboardViewModel
import com.colimator.app.viewmodel.ImagesViewModel
import com.colimator.app.viewmodel.OnboardingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

fun main() = application {
    // Dependency Injection Root
    val shellExecutor = JvmShellExecutor()
    val colimaService = ColimaService(shellExecutor)
    val dockerService = DockerService(shellExecutor)
    val onboardingViewModel = OnboardingViewModel(colimaService, dockerService)
    val dashboardViewModel = DashboardViewModel(colimaService)
    val containersViewModel = ContainersViewModel(dockerService)
    val imagesViewModel = ImagesViewModel(dockerService)
    
    // Coroutine scope for tray actions
    val trayScope = CoroutineScope(Dispatchers.IO)
    
    // Window state - use visible property instead of conditional rendering
    var isWindowVisible by remember { mutableStateOf(true) }
    val windowState = remember { WindowState() }
    
    // System Tray setup
    if (SystemTray.isSupported()) {
        val tray = SystemTray.getSystemTray()
        
        // Create a simple icon (green/gray square for MVP)
        val icon = createTrayIcon()
        
        val popup = PopupMenu().apply {
            add(MenuItem("Start Colima").apply {
                addActionListener {
                    trayScope.launch { colimaService.start() }
                    dashboardViewModel.refreshStatus()
                }
            })
            add(MenuItem("Stop Colima").apply {
                addActionListener {
                    trayScope.launch { colimaService.stop() }
                    dashboardViewModel.refreshStatus()
                }
            })
            addSeparator()
            add(MenuItem("Show Window").apply {
                addActionListener {
                    isWindowVisible = true
                    windowState.isMinimized = false
                    windowState.placement = WindowPlacement.Floating
                }
            })
            addSeparator()
            add(MenuItem("Quit").apply {
                addActionListener {
                    exitApplication()
                }
            })
        }
        
        val trayIcon = TrayIcon(icon, "Colimator", popup).apply {
            isImageAutoSize = true
            addActionListener {
                // Left-click: show window
                isWindowVisible = true
                windowState.isMinimized = false
                windowState.placement = WindowPlacement.Floating
            }
        }
        
        try {
            tray.add(trayIcon)
        } catch (e: Exception) {
            // Tray not available, continue without it
            println("System tray not available: ${e.message}")
        }
    }

    // Main window - always rendered, visibility controlled
    Window(
        onCloseRequest = {
            // Minimize to tray instead of exiting
            if (SystemTray.isSupported()) {
                isWindowVisible = false
            } else {
                exitApplication()
            }
        },
        visible = isWindowVisible,
        state = windowState,
        title = "Colimator",
    ) {
        App(
            onboardingViewModel = onboardingViewModel,
            dashboardViewModel = dashboardViewModel,
            containersViewModel = containersViewModel,
            imagesViewModel = imagesViewModel,
            onExit = ::exitApplication
        )
    }
}

/**
 * Creates a simple 22x22 tray icon for macOS.
 * Uses a teal/green color to match the app theme.
 */
private fun createTrayIcon(): BufferedImage {
    val size = 22
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    
    // Draw a simple rounded rectangle in teal color
    g.color = java.awt.Color(0x80, 0xCB, 0xC4) // Teal from our theme
    g.fillRoundRect(2, 2, size - 4, size - 4, 4, 4)
    
    // Draw a "C" letter for Colimator
    g.color = java.awt.Color.WHITE
    g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 14)
    val metrics = g.fontMetrics
    val x = (size - metrics.stringWidth("C")) / 2
    val y = (size - metrics.height) / 2 + metrics.ascent
    g.drawString("C", x, y)
    
    g.dispose()
    return image
}
