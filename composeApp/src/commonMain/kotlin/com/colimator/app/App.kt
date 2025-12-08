package com.colimator.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import com.colimator.app.ui.theme.ColimatorTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Layers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.colimator.app.ui.ContainersScreen
import com.colimator.app.ui.DashboardScreen
import com.colimator.app.ui.ImagesScreen
import com.colimator.app.ui.OnboardingScreen
import com.colimator.app.viewmodel.ContainersViewModel
import com.colimator.app.viewmodel.DashboardViewModel
import com.colimator.app.viewmodel.ImagesViewModel
import com.colimator.app.viewmodel.OnboardingViewModel

enum class Screen {
    Onboarding, Dashboard, Containers, Images
}

@Composable
fun App(
    onboardingViewModel: OnboardingViewModel,
    dashboardViewModel: DashboardViewModel,
    containersViewModel: ContainersViewModel,
    imagesViewModel: ImagesViewModel,
    onExit: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.Onboarding) }

    ColimatorTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.Onboarding -> {
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onReady = { currentScreen = Screen.Dashboard },
                        onExit = onExit
                    )
                }
                
                Screen.Dashboard, Screen.Containers, Screen.Images -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = currentScreen == Screen.Dashboard,
                                onClick = { currentScreen = Screen.Dashboard },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") }
                            )
                            NavigationRailItem(
                                selected = currentScreen == Screen.Containers,
                                onClick = { currentScreen = Screen.Containers },
                                icon = { Icon(Icons.Default.List, contentDescription = "Containers") },
                                label = { Text("Containers") }
                            )
                            NavigationRailItem(
                                selected = currentScreen == Screen.Images,
                                onClick = { currentScreen = Screen.Images },
                                icon = { Icon(Icons.Default.Layers, contentDescription = "Images") },
                                label = { Text("Images") }
                            )
                        }

                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(dashboardViewModel)
                            Screen.Containers -> ContainersScreen(containersViewModel)
                            Screen.Images -> ImagesScreen(imagesViewModel)
                            else -> { /* handled above */ }
                        }
                    }
                }
            }
        }
    }
}
