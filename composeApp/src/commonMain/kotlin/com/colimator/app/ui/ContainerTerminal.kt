package com.colimator.app.ui

import androidx.compose.runtime.Composable

@Composable
expect fun ContainerTerminal(
    containerId: String,
    profileName: String?,
    onTerminalSessionActive: (Boolean) -> Unit
)
