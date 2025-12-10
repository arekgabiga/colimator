package com.colimator.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import com.colimator.app.model.Container
import com.colimator.app.viewmodel.ContainerSortField
import com.colimator.app.viewmodel.ContainersViewModel
import com.colimator.app.viewmodel.SortDirection

/**
 * Screen displaying Docker containers list with sorting and actions.
 */
@Composable
fun ContainersScreen(
    viewModel: ContainersViewModel,
    onContainerClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    
    // Scroll to top when sort changes
    LaunchedEffect(state.sortVersion) {
        listState.animateScrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Containers",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Column headers for sorting
            ContainerSortableHeader(
                currentField = state.sortField,
                currentDirection = state.sortDirection,
                onSortChange = { viewModel.setSortField(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Loading indicator
            if (state.isLoading && state.containers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Empty state
            else if (state.containers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No containers found.\nMake sure Docker is running.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Container list
            else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.containers, key = { it.id }) { container ->
                        ContainerRow(
                            container = container,
                            isLoading = state.isLoading,
                            onStart = { viewModel.startContainer(container.id) },
                            onStop = { viewModel.stopContainer(container.id) },
                            onRemove = { viewModel.removeContainer(container.id) },
                            onTerminal = { onContainerClick(container.id) }
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
    }
}

/**
 * Sortable column headers for containers table.
 */
@Composable
private fun ContainerSortableHeader(
    currentField: ContainerSortField,
    currentDirection: SortDirection,
    onSortChange: (ContainerSortField) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name column
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onSortChange(ContainerSortField.NAME) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (currentField == ContainerSortField.NAME) FontWeight.Bold else FontWeight.Normal
            )
            if (currentField == ContainerSortField.NAME) {
                Text(
                    text = if (currentDirection == SortDirection.ASC) " ▲" else " ▼",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Status column
        Row(
            modifier = Modifier
                .width(80.dp)
                .clickable { onSortChange(ContainerSortField.STATUS) },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (currentField == ContainerSortField.STATUS) FontWeight.Bold else FontWeight.Normal
            )
            if (currentField == ContainerSortField.STATUS) {
                Text(
                    text = if (currentDirection == SortDirection.ASC) " ▲" else " ▼",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Actions column (no sorting)
        Text(
            text = "Actions",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(140.dp)
        )
    }
}

/**
 * Single container row with status indicator and action buttons.
 */
@Composable
private fun ContainerRow(
    container: Container,
    isLoading: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRemove: () -> Unit,
    onTerminal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Container name and details (matches Name column)
            Box(modifier = Modifier.weight(1f)) {
                SelectionContainer {
                    Column {
                        Text(
                            text = container.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = container.image,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (container.ports.isNotBlank()) {
                            Text(
                                text = container.ports,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Status indicator (matches Status column - 80.dp width)
            Box(
                modifier = Modifier.width(80.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (container.isRunning) "🟢 Running" else "🔴 Stopped",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Action buttons (matches Actions column - 140.dp width)
            Row(
                modifier = Modifier.width(140.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // Terminal button - only for running containers
                IconButton(
                    onClick = onTerminal,
                    enabled = !isLoading && container.isRunning
                ) {
                    Icon(
                        Icons.Outlined.Terminal,
                        contentDescription = "Terminal",
                        tint = if (container.isRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                if (container.isRunning) {
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
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = Color(0xFF4CAF50) // Green
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    enabled = !isLoading && !container.isRunning
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (!container.isRunning) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

