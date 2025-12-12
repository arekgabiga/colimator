package com.colimator.app.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colimator.app.util.AnsiParser
import com.colimator.app.viewmodel.ContainersViewModel
import kotlinx.coroutines.launch

@Composable
fun ContainerLogsTab(
    containerId: String,
    viewModel: ContainersViewModel
) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    // Auto-scroll logic
    val isUserScrolling = listState.isScrollInProgress
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf true
            
            val lastVisibleItem = visibleItemsInfo.last()
            val totalItems = layoutInfo.totalItemsCount
            
            lastVisibleItem.index == totalItems - 1
        }
    }
    
    // Start/Stop streaming
    DisposableEffect(containerId) {
        viewModel.startStreamingLogs(containerId)
        onDispose {
            viewModel.stopStreamingLogs()
        }
    }
    
    // Auto-scroll effect
    // We want to scroll to bottom if:
    // 1. We are already at the bottom (isAtBottom && !isUserScrolling)
    // 2. It is the initial load of logs (we haven't scrolled yet)
    
    var hasInitialScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            if (!hasInitialScrolled) {
                // Initial jump to bottom
                listState.scrollToItem(logs.size - 1)
                hasInitialScrolled = true
            } else if (!isUserScrolling && isAtBottom) {
                // Formatting update or new log, animate to bottom
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Warning: Logs are ephemeral and not persisted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            
            IconButton(
                onClick = {
                    val text = logs.joinToString("\n") { "${it.timestamp} ${it.content}" }
                    clipboardManager.setText(AnnotatedString(text))
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy, 
                    contentDescription = "Copy Logs",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        


        Box(modifier = Modifier.fillMaxSize()) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E)) // Dark background for logs
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = logs,
                        key = { it.id }
                    ) { log ->
                        // Parse ANSI codes
                        val styledContent = remember(log.content) {
                            AnsiParser.parse(log.content)
                        }
                        
                        Row {
                            // Timestamp
                            if (log.timestamp.isNotBlank()) {
                                Text(
                                    text = log.timestamp.take(19) + " ", // roughly 2023-10-10T10:00:00
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            
                            // Content
                            Text(
                                text = styledContent,
                                color = Color.Unspecified, // Let AnsiParser handle colors or default to Theme
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            
                            // Invisible newline for copy-paste
                            Text(
                                text = "\n",
                                modifier = Modifier.size(1.dp),
                                color = Color.Transparent,
                                fontSize = 1.sp
                            )
                        }
                    }
                }
            }
            
            // Scroll to bottom button if not at bottom
            if (!isAtBottom) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (logs.isNotEmpty()) {
                                listState.animateScrollToItem(logs.size - 1)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Scroll to Bottom")
                }
            }
            
            if (logs.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Waiting for logs...", color = Color.Gray)
                 }
            }
        }
    }
}
