package com.colimator.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colimator.app.model.MountType
import com.colimator.app.model.Profile
import com.colimator.app.model.ProfileCreateConfig
import com.colimator.app.model.VmType
import com.colimator.app.service.VmStatus
import com.colimator.app.viewmodel.ProfilesViewModel

@Composable
fun ProfilesScreen(viewModel: ProfilesViewModel) {
    val state by viewModel.state.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profiles",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshProfiles() },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    Button(
                        onClick = { viewModel.showCreateDialog() },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Profile")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading or content
            if (state.isLoading && state.profiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.profiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No profiles found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.showCreateDialog() }) {
                            Text("Create your first profile")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.profiles) { profile ->
                        val isActiveProfile = (state.activeProfileName ?: "default") == profile.name
                        val isStarting = state.startingProfileName == profile.name
                        val isStopping = state.stoppingProfileName == profile.name
                        ProfileCard(
                            profile = profile,
                            isActiveProfile = isActiveProfile,
                            isStarting = isStarting,
                            isStopping = isStopping,
                            onStart = { viewModel.startProfile(profile) },
                            onStop = { viewModel.stopProfile(profile) },
                            onSwitch = { viewModel.switchToProfile(profile) },
                            onConfigure = { viewModel.openConfigDialog(profile) },
                            onDelete = { viewModel.confirmDeleteProfile(profile) },
                            isLoading = state.isLoading
                        )
                    }
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

        // Advanced Config Dialog
        state.editingProfile?.let { profile ->
            state.editingConfig?.let { config ->
                AdvancedConfigDialog(
                    initialConfig = config,
                    maxCpu = state.hostMaxCpu,
                    maxMemory = state.hostMaxMemory,
                    isExistingProfile = true,
                    onDismiss = { viewModel.closeConfigDialog() },
                    onSave = { newConfig -> viewModel.saveConfig(newConfig, shouldRestart = true) }
                )
            }
        }
        
        // Create dialog
        if (state.showCreateDialog) {
            CreateProfileDialog(
                config = state.createConfig,
                maxCpu = state.hostMaxCpu,
                maxMemory = state.hostMaxMemory,
                onConfigChange = { viewModel.updateCreateConfig(it) },
                onDismiss = { viewModel.hideCreateDialog() },
                onCreate = { viewModel.createProfile() },
                isCreating = state.isCreating
            )
        }
        
        // Delete confirmation dialog
        state.profileToDelete?.let { profile ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("Delete Profile") },
                text = { 
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Profile: ${profile.name}",
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
                        onClick = { viewModel.deleteProfile() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActiveProfile: Boolean,
    isStarting: Boolean,
    isStopping: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSwitch: () -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (profile.status) {
                                    VmStatus.Running -> Color(0xFF4CAF50) // Green
                                    VmStatus.Starting, VmStatus.Stopping -> Color(0xFFFFC107) // Yellow
                                    else -> Color.Gray
                                }
                            )
                    )
                    
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isActiveProfile) {
                                Text(
                                    text = "(active)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = when {
                                isStarting -> "Starting..."
                                isStopping -> "Stopping..."
                                else -> profile.status.name
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Actions - consistent layout: [Make active] [Start/Stop] [Delete]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slot 1: Make active button
                    Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                        if (profile.status == VmStatus.Running && !isActiveProfile) {
                            TextButton(
                                onClick = onSwitch,
                                enabled = !isLoading,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Make active", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Slot 2: Settings (Gear)
                    IconButton(
                        onClick = onConfigure,
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    
                    // Slot 3: Start/Stop button
                    if (profile.status == VmStatus.Running) {
                        IconButton(
                            onClick = onStop,
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Stop, 
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onStart,
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                        }
                    }
                    
                    // Slot 4: Delete button - available for all profiles
                    IconButton(
                        onClick = onDelete,
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Config info if available
            if (profile.cpuCores > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${profile.cpuCores} CPU",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f GB RAM".format(profile.memoryGb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f GB Disk".format(profile.diskGb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProfileDialog(
    config: ProfileCreateConfig,
    maxCpu: Int,
    maxMemory: Int,
    onConfigChange: (ProfileCreateConfig) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    isCreating: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp,
            modifier = Modifier.width(500.dp)
        ) {
            Box(modifier = Modifier.heightIn(max = 700.dp)) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(scrollState)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create New Profile",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    HorizontalDivider()

                    // Profile name
                    OutlinedTextField(
                        value = config.name,
                        onValueChange = { onConfigChange(config.copy(name = it)) },
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isCreating
                    )
                    
                    
                    HorizontalDivider()
                    
                    Text(
                        text = "Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                        // CPU
                        ResourceSlider(
                            label = "CPU Cores",
                            value = config.cpu,
                            rawMax = maxCpu,
                            onValueChange = { onConfigChange(config.copy(cpu = it)) }
                        )
                        
                        // Memory
                        ResourceSlider(
                            label = "Memory (GB)",
                            value = config.memory,
                            rawMax = maxMemory,
                            onValueChange = { onConfigChange(config.copy(memory = it)) }
                        )
                        
                        // Disk
                        OutlinedTextField(
                            value = config.disk.toString(),
                            onValueChange = { 
                                val newSize = it.toIntOrNull()
                                // Only allow reasonably positive values
                                if (newSize != null && newSize > 0) {
                                    onConfigChange(config.copy(disk = newSize))
                                }
                            },
                            label = { Text("Disk Size (GiB)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("Min 10 GiB") },
                            enabled = !isCreating
                        )
                        
                        // VM Type dropdown
                        var vmTypeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = vmTypeExpanded,
                            onExpandedChange = { vmTypeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = config.vmType.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("VM Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vmTypeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                enabled = !isCreating
                            )
                            ExposedDropdownMenu(
                                expanded = vmTypeExpanded,
                                onDismissRequest = { vmTypeExpanded = false }
                            ) {
                                VmType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        onClick = {
                                            onConfigChange(config.copy(vmType = type))
                                            vmTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Mount Type dropdown
                        var mountTypeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = mountTypeExpanded,
                            onExpandedChange = { mountTypeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = config.mountType.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mount Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mountTypeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                enabled = !isCreating
                            )
                            ExposedDropdownMenu(
                                expanded = mountTypeExpanded,
                                onDismissRequest = { mountTypeExpanded = false }
                            ) {
                                MountType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        onClick = {
                                            onConfigChange(config.copy(mountType = type))
                                            mountTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Kubernetes toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Kubernetes")
                            Switch(
                                checked = config.kubernetes,
                                onCheckedChange = { onConfigChange(config.copy(kubernetes = it)) },
                                enabled = !isCreating
                            )
                        }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isCreating
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onCreate,
                            enabled = !isCreating && config.name.isNotBlank()
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Create & Start")
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState),
                    style = rememberVisibleScrollbarStyle()
                )
            }
        }
    }
}

