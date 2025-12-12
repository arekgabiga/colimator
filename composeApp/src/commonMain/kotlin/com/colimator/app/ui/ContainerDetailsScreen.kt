package com.colimator.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailsScreen(
    containerId: String,
    profileName: String?,
    dockerService: com.colimator.app.service.DockerService,
    containersViewModel: com.colimator.app.viewmodel.ContainersViewModel,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isTerminalConnected by remember { mutableStateOf(hasActiveTerminalSession(containerId)) }
    
    // Tabs: Info, Logs, Terminal
    val tabs = listOf("Info", "Logs", "Terminal")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Container Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                // Show indicator when terminal has active session
                                // Terminal is now at index 2
                                if (index == 2 && isTerminalConnected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("●", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> ContainerInfoTab(
                        containerId = containerId,
                        profileName = profileName,
                        dockerService = dockerService
                    )
                    1 -> ContainerLogsTab(
                        containerId = containerId,
                        viewModel = containersViewModel
                    )
                    2 -> TerminalTabContent(
                        containerId = containerId,
                        profileName = profileName,
                        isConnected = isTerminalConnected,
                        onConnectionStateChange = { connected -> isTerminalConnected = connected }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalTabContent(
    containerId: String,
    profileName: String?,
    isConnected: Boolean,
    onConnectionStateChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with disconnect/reconnect button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isConnected) {
                OutlinedButton(
                    onClick = {
                        disconnectTerminalSession(containerId)
                        onConnectionStateChange(false)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect")
                }
            } else {
                Button(
                    onClick = {
                        // The session will be recreated when ContainerTerminal is rendered
                        onConnectionStateChange(true)
                    }
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect")
                }
            }
        }
        
        // Terminal content
        Box(modifier = Modifier.fillMaxSize()) {
            if (isConnected) {
                ContainerTerminal(
                    containerId = containerId,
                    profileName = profileName,
                    onTerminalSessionActive = { active -> 
                        if (!active) onConnectionStateChange(false)
                    }
                )
            } else {
                // Show disconnected message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Terminal disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Click Connect to start a new session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Check if there's an active terminal session for this container.
 * This is implemented in desktopMain via expect/actual.
 */
expect fun hasActiveTerminalSession(containerId: String): Boolean

/**
 * Disconnect an active terminal session.
 * This is implemented in desktopMain via expect/actual.
 */
expect fun disconnectTerminalSession(containerId: String)
