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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import com.colimator.app.model.DockerImageWithUsage
import com.colimator.app.viewmodel.ImageSortField
import com.colimator.app.viewmodel.ImagesViewModel
import com.colimator.app.viewmodel.SortDirection

/**
 * Screen displaying Docker images list with sorting.
 */
@Composable
fun ImagesScreen(viewModel: ImagesViewModel) {
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
                    text = "Images",
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
            SortableHeader(
                currentField = state.sortField,
                currentDirection = state.sortDirection,
                onSortChange = { viewModel.setSortField(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Loading indicator
            if (state.isLoading && state.images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Empty state
            else if (state.images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No images found.\nMake sure Docker is running.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Images list
            else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.images, key = { it.image.id }) { imageWithUsage ->
                        ImageRow(imageWithUsage = imageWithUsage)
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
 * Sortable column headers.
 */
@Composable
private fun SortableHeader(
    currentField: ImageSortField,
    currentDirection: SortDirection,
    onSortChange: (ImageSortField) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Name column
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onSortChange(ImageSortField.NAME) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (currentField == ImageSortField.NAME) FontWeight.Bold else FontWeight.Normal
            )
            if (currentField == ImageSortField.NAME) {
                Text(
                    text = if (currentDirection == SortDirection.ASC) " ▲" else " ▼",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Size column
        Row(
            modifier = Modifier
                .width(80.dp)
                .clickable { onSortChange(ImageSortField.SIZE) },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Size",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (currentField == ImageSortField.SIZE) FontWeight.Bold else FontWeight.Normal
            )
            if (currentField == ImageSortField.SIZE) {
                Text(
                    text = if (currentDirection == SortDirection.ASC) " ▲" else " ▼",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // In Use column (no sorting)
        Text(
            text = "In Use",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(60.dp)
        )
    }
}

/**
 * Single image row displaying ID, name, size, and usage status.
 */
@Composable
private fun ImageRow(imageWithUsage: DockerImageWithUsage) {
    val image = imageWithUsage.image
    
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
            // Image details
            Box(modifier = Modifier.weight(1f)) {
                SelectionContainer {
                    Column {
                        Text(
                            text = image.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = image.shortId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Size
            Text(
                text = image.size,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(80.dp)
            )

            // In use indicator
            Box(
                modifier = Modifier.width(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (imageWithUsage.isInUse) "🟢" else "⚪",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
