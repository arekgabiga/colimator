package com.colimator.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.colimator.app.domain.ColimaConfig
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.math.roundToInt

@Composable
fun AdvancedConfigDialog(
    initialConfig: ColimaConfig,
    maxCpu: Int,
    maxMemory: Int,
    isExistingProfile: Boolean,
    onDismiss: () -> Unit,
    onSave: (ColimaConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    Dialog(onDismissRequest = {
        if (config != initialConfig) {
            showDiscardConfirmation = true
        } else {
            onDismiss()
        }
    }) {
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
                        text = "Configuration",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    HorizontalDivider()

                    // CPU
                    ResourceSlider(
                        label = "CPU Cores",
                        value = config.cpu,
                        rawMax = maxCpu,
                        onValueChange = { config = config.copy(cpu = it) }
                    )

                    // Memory
                    ResourceSlider(
                        label = "Memory (GiB)",
                        value = config.memory,
                        rawMax = maxMemory,
                        onValueChange = { config = config.copy(memory = it) }
                    )

                    // Disk
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = config.disk.toString(),
                            onValueChange = { 
                                val newSize = it.toIntOrNull()
                                if (newSize != null) {
                                    // Only allow updates if acceptable
                                    // For existing profiles, block shrinking
                                    if (!isExistingProfile || newSize >= initialConfig.disk) {
                                        config = config.copy(disk = newSize)
                                    }
                                }
                            },
                            label = { Text("Disk Size (GiB)") },
                            modifier = Modifier.weight(1f),
                            isError = isExistingProfile && config.disk < initialConfig.disk,
                            supportingText = {
                                if (isExistingProfile) Text("Cannot shrink disk of existing profile")
                            }
                        )
                    }

                    // Kubernetes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Kubernetes", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Enable local K8s cluster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.kubernetes,
                            onCheckedChange = { config = config.copy(kubernetes = it) }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                             if (config != initialConfig) {
                                showDiscardConfirmation = true
                            } else {
                                onDismiss()
                            }
                        }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onSave(config) }) {
                            Text("Save")
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

