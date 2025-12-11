package com.colimator.app.ui

/**
 * Check if there's an active terminal session for this container.
 */
actual fun hasActiveTerminalSession(containerId: String): Boolean {
    return TerminalSessionManager.hasActiveSession(containerId)
}

/**
 * Disconnect an active terminal session.
 */
actual fun disconnectTerminalSession(containerId: String) {
    TerminalSessionManager.closeSession(containerId)
}
