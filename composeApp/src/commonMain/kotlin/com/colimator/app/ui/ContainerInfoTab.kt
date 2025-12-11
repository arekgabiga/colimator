package com.colimator.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colimator.app.model.ContainerInspection
import com.colimator.app.service.DockerService
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun ContainerInfoTab(
    containerId: String,
    profileName: String?,
    dockerService: DockerService
) {
    var inspection by remember { mutableStateOf<ContainerInspection?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showRaw by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    fun loadData() {
        scope.launch {
            isLoading = true
            inspection = dockerService.inspectContainer(containerId, profileName)
            isLoading = false
        }
    }

    LaunchedEffect(containerId) {
        loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (inspection == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Failed to load container info",
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = { loadData() }) {
                    Text("Retry")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    
                    OutlinedButton(onClick = { showRaw = !showRaw }) {
                        Icon(if (showRaw) Icons.Default.Visibility else Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showRaw) "View Overview" else "View Raw JSON")
                    }
                }
                
                if (showRaw) {
                    RawJsonView(inspection!!)
                } else {
                    OverviewView(inspection!!)
                }
            }
        }
    }
}

@Composable
private fun OverviewView(inspection: ContainerInspection) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Section
        HeroSection(inspection)
        
        // Environment Variables
        if (inspection.config.env.isNotEmpty()) {
            Section("Environment Variables") {
                EnvVarsTable(inspection.config.env)
            }
        }
        
        // Network
        Section("Network") {
            NetworkInfo(inspection)
        }
        
        // Mounts
        if (inspection.mounts.isNotEmpty()) {
            Section("Mounts") {
                MountsList(inspection.mounts)
            }
        }
    }
}

@Composable
private fun HeroSection(inspection: ContainerInspection) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isRunning = inspection.state.running
                Icon(
                    if (isRunning) Icons.Default.PlayCircle else Icons.Default.StopCircle,
                    contentDescription = null,
                    tint = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = inspection.state.status.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "ID: ${inspection.id.take(12)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoRow("Image", inspection.config.image)
            InfoRow("Command", inspection.config.cmd?.joinToString(" ") ?: "-")
            InfoRow("Created", inspection.created)
            InfoRow("IP Address", inspection.networkSettings.ipAddress.ifEmpty { "N/A" })
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun EnvVarsTable(envVars: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            envVars.sorted().forEachIndexed { index, env ->
                val parts = env.split("=", limit = 2)
                val key = parts.getOrNull(0) ?: ""
                val value = parts.getOrNull(1) ?: ""
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(180.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionContainer {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (index < envVars.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun NetworkInfo(inspection: ContainerInspection) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoRow("Gateway", inspection.networkSettings.gateway)
            inspection.networkSettings.ports.forEach { (port, bindings) ->
                val hostPart = bindings?.firstOrNull()?.let { "${it.hostIp}:${it.hostPort}" } ?: "Not bound"
                InfoRow("Port $port", hostPart)
            }
        }
    }
}

@Composable
private fun MountsList(mounts: List<com.colimator.app.model.Mount>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        mounts.forEach { mount ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mount.type.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (mount.rw) "RW" else "RO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(
                                "${mount.source} : ${mount.destination}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RawJsonView(inspection: ContainerInspection) {
    val jsonString = remember(inspection) {
        val json = Json { prettyPrint = true }
        json.encodeToString(inspection)
    }
    
    val clipboardManager = LocalClipboardManager.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = jsonString,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        FloatingActionButton(
            onClick = { clipboardManager.setText(AnnotatedString(jsonString)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.ContentCopy, "Copy JSON")
        }
    }
}
