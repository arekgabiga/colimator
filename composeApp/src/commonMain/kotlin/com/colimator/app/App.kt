package com.colimator.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.colimator.app.ui.DashboardScreen
import com.colimator.app.viewmodel.DashboardViewModel

enum class Screen {
    Dashboard, Containers
}

@Composable
fun App(dashboardViewModel: DashboardViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
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
                }

                when (currentScreen) {
                    Screen.Dashboard -> DashboardScreen(dashboardViewModel)
                    Screen.Containers -> {
                        // Placeholder for Containers Screen
                        Text("Containers List (ToDo)")
                    }
                }
            }
        }
    }
}
