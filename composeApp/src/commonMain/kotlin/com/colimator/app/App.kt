package com.colimator.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.colimator.app.model.Profile
import com.colimator.app.service.ActiveProfileRepository
import com.colimator.app.service.VmStatus
import com.colimator.app.ui.ContainerDetailsScreen
import com.colimator.app.ui.ContainerDetailsScreen
import com.colimator.app.ui.ContainersScreen
import com.colimator.app.ui.DashboardScreen
import com.colimator.app.ui.ImagesScreen
import com.colimator.app.ui.OnboardingScreen
import com.colimator.app.ui.ProfilesScreen
import com.colimator.app.ui.theme.ColimatorTheme
import com.colimator.app.viewmodel.ContainersViewModel
import com.colimator.app.viewmodel.DashboardViewModel
import com.colimator.app.viewmodel.ImagesViewModel
import com.colimator.app.viewmodel.OnboardingViewModel
import com.colimator.app.viewmodel.ProfilesViewModel

sealed class Screen {
    data object Onboarding : Screen()
    data object Dashboard : Screen()
    data object Containers : Screen()
    data object Images : Screen()
    data object Profiles : Screen()
    data class ContainerDetails(val containerId: String) : Screen()
}

@Composable
fun App(
    onboardingViewModel: OnboardingViewModel,
    dashboardViewModel: DashboardViewModel,
    containersViewModel: ContainersViewModel,
    imagesViewModel: ImagesViewModel,
    profilesViewModel: ProfilesViewModel,
    activeProfileRepository: ActiveProfileRepository,
    dockerService: com.colimator.app.service.DockerService,
    onExit: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }

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
                
                Screen.Dashboard, Screen.Containers, Screen.Images, Screen.Profiles, is Screen.ContainerDetails -> {
                    // Trigger refresh when navigating to Containers or Images
                    LaunchedEffect(currentScreen) {
                        if ((currentScreen is Screen.Containers || currentScreen is Screen.ContainerDetails) && 
                            previousScreen !is Screen.Containers && previousScreen !is Screen.ContainerDetails) {
                            containersViewModel.onScreenVisible()
                        } else if (currentScreen is Screen.Images && previousScreen !is Screen.Images) {
                            imagesViewModel.onScreenVisible()
                        }
                        previousScreen = currentScreen
                    }
                    
                    // Get profiles for the selector
                    val profilesState by profilesViewModel.state.collectAsState()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top bar with profile selector
                        TopBar(
                            profiles = profilesState.profiles,
                            activeProfileRepository = activeProfileRepository,
                            onRefreshProfiles = { profilesViewModel.refreshProfiles() }
                        )
                        
                        // Main content with navigation rail
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail {
                                NavigationRailItem(
                                    selected = currentScreen == Screen.Dashboard,
                                    onClick = { currentScreen = Screen.Dashboard },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard") }
                                )
                                NavigationRailItem(
                                    selected = currentScreen is Screen.Containers || currentScreen is Screen.ContainerDetails,
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
                                NavigationRailItem(
                                    selected = currentScreen == Screen.Profiles,
                                    onClick = { currentScreen = Screen.Profiles },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Profiles") },
                                    label = { Text("Profiles") }
                                )
                            }

                            when (currentScreen) {
                                Screen.Dashboard -> DashboardScreen(dashboardViewModel)
                                Screen.Containers -> ContainersScreen(
                                    viewModel = containersViewModel,
                                    onContainerClick = { id -> currentScreen = Screen.ContainerDetails(id) }
                                )
                                is Screen.ContainerDetails -> ContainerDetailsScreen(
                                    containerId = (currentScreen as Screen.ContainerDetails).containerId,
                                    profileName = activeProfileRepository.activeProfile.value,
                                    dockerService = dockerService,
                                    onBack = { currentScreen = Screen.Containers }
                                )
                                Screen.Images -> ImagesScreen(imagesViewModel)
                                Screen.Profiles -> ProfilesScreen(profilesViewModel)
                                else -> { /* handled above */ }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    profiles: List<Profile>,
    activeProfileRepository: ActiveProfileRepository,
    onRefreshProfiles: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileSelector(
                profiles = profiles,
                activeProfileRepository = activeProfileRepository,
                onRefresh = onRefreshProfiles
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSelector(
    profiles: List<Profile>,
    activeProfileRepository: ActiveProfileRepository,
    onRefresh: () -> Unit
) {
    val activeProfile by activeProfileRepository.activeProfile.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    
    // Find the active profile object
    val currentProfile = profiles.find { 
        it.name == (activeProfile ?: "default") 
    }
    
    // Refresh profiles on first composition
    LaunchedEffect(Unit) {
        onRefresh()
    }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // Profile button
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.menuAnchor(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (currentProfile?.status) {
                            VmStatus.Running -> Color(0xFF4CAF50)
                            VmStatus.Starting, VmStatus.Stopping -> Color(0xFFFFC107)
                            else -> Color.Gray
                        }
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Profile name
            Text(
                text = activeProfile ?: "default",
                style = MaterialTheme.typography.labelMedium
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select profile",
                modifier = Modifier.size(18.dp)
            )
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 150.dp, max = 250.dp)
        ) {
            if (profiles.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No profiles available") },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (profile.status) {
                                                VmStatus.Running -> Color(0xFF4CAF50)
                                                else -> Color.Gray
                                            }
                                        )
                                )
                                Text(profile.displayName)
                            }
                        },
                        onClick = {
                            activeProfileRepository.setActiveProfile(
                                if (profile.name == "default") null else profile.name
                            )
                            expanded = false
                        },
                        trailingIcon = {
                            if (profile.name == (activeProfile ?: "default")) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }
    }
}



