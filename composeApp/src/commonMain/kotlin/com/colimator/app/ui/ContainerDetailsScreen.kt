package com.colimator.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailsScreen(
    containerId: String,
    profileName: String?,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isTerminalActive by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Info", "Terminal")

    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Active Terminal Session") },
            text = { Text("You have an active terminal session. Disconnecting will terminate the process. Are you sure?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Container Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isTerminalActive) {
                            showExitDialog = true
                        } else {
                            onBack()
                        }
                    }) {
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
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        Text("Container Info for $containerId (Coming Soon)") 
                    }
                    1 -> ContainerTerminal(
                        containerId = containerId,
                        profileName = profileName,
                        onTerminalSessionActive = { isActive -> isTerminalActive = isActive }
                    )
                }
            }
        }
    }
}
