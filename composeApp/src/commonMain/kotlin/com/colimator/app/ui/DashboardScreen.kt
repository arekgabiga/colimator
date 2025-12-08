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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.colimator.app.service.ColimaConfig
import com.colimator.app.service.VmStatus
import com.colimator.app.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Colima Status (${state.activeProfile}): ${state.vmStatus}",
                    style = MaterialTheme.typography.headlineMedium
                )
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // VM Configuration card (only when running)
            state.vmConfig?.let { config ->
                VmConfigCard(config)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = viewModel::startVm,
                    enabled = !state.isLoading && 
                        (state.vmStatus == VmStatus.Stopped || state.vmStatus == VmStatus.Unknown)
                ) {
                    Text("Start")
                }

                Button(
                    onClick = viewModel::stopVm,
                    enabled = !state.isLoading && state.vmStatus == VmStatus.Running
                ) {
                    Text("Stop")
                }

                Button(
                    onClick = viewModel::refreshStatus,
                    enabled = !state.isLoading
                ) {
                    Text("Refresh")
                }
                
                // Delete button - available for all profiles
                Button(
                    onClick = viewModel::confirmDeleteVm,
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete VM")
                }
            }
        }

        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
        
        // Delete confirmation dialog
        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteVm() },
                title = { Text("Delete VM") },
                text = { 
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Profile: ${state.activeProfile}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text("Deleting this profile will permanently remove:")
                        Text("• The virtual machine (VM) for this profile")
                        Text("• All Docker containers inside this VM")
                        Text("• All Docker images stored in this VM")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This action cannot be undone.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteVm() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteVm() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun VmConfigCard(config: ColimaConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "VM Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConfigColumn(
                    items = listOf(
                        "CPU" to "${config.cpuCores} cores",
                        "Memory" to "%.1f GB".format(config.memoryGb),
                        "Disk" to "%.0f GB".format(config.diskGb)
                    )
                )
                ConfigColumn(
                    items = listOf(
                        "Architecture" to config.architecture,
                        "Runtime" to config.runtime,
                        "Mount Type" to config.mountType
                    )
                )
            }
            
            // Driver at the bottom (full width as it can be long)
            ConfigItem("Driver", config.driver)
            
            if (config.kubernetes) {
                ConfigItem("Kubernetes", "Enabled")
            }
        }
    }
}

@Composable
private fun ConfigColumn(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            ConfigItem(label, value)
        }
    }
}

@Composable
private fun ConfigItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

