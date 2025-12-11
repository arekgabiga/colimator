package com.colimator.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel

@Composable
actual fun ContainerTerminal(
    containerId: String,
    profileName: String?,
    onTerminalSessionActive: (Boolean) -> Unit
) {
    // Get or create a session from the manager
    val session = remember(containerId) {
        TerminalSessionManager.getOrCreateSession(
            containerId = containerId,
            profileName = profileName,
            onSessionCreated = { onTerminalSessionActive(true) }
        )
    }
    
    // Update activity status and request focus when returning to this terminal
    LaunchedEffect(containerId) {
        if (session.isAlive) {
            onTerminalSessionActive(true)
            session.widget.requestFocusInWindow()
        }
    }
    
    // Note: We do NOT clean up on dispose - the session persists!
    // Session cleanup happens via TerminalSessionManager.closeSession()
    
    DisposableEffect(containerId) {
        onDispose {
            // Don't terminate the session, just mark as not visible
            // The session continues to run in the background
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = { session.widget }
    )
}
