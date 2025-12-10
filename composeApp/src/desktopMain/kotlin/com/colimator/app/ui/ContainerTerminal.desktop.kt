package com.colimator.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.jediterm.pty.PtyProcessTtyConnector
import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import com.pty4j.PtyProcessBuilder
import java.awt.Color
import java.nio.charset.StandardCharsets
import javax.swing.JScrollBar
import javax.swing.plaf.basic.BasicScrollBarUI

@Composable
actual fun ContainerTerminal(
    containerId: String,
    profileName: String?,
    onTerminalSessionActive: (Boolean) -> Unit
) {
    // Dark theme settings matching the app's color scheme
    val settings = remember { 
        object : DefaultSettingsProvider() {
            // Dark background (#121212) with light grey text (#E3E3E3) - matches app theme
            override fun getDefaultStyle(): TextStyle {
                return TextStyle(
                    TerminalColor.rgb(227, 227, 227),  // onBackground: Light grey text
                    TerminalColor.rgb(18, 18, 18)       // background: Near-black (#121212)
                )
            }
            
            override fun getSelectionColor(): TextStyle {
                return TextStyle(
                    TerminalColor.WHITE,
                    TerminalColor.rgb(0, 72, 128)  // primaryContainer color for selection
                )
            }
            
            override fun getTerminalFont(): java.awt.Font {
                return java.awt.Font("Menlo", java.awt.Font.PLAIN, 13)
            }
        }
    }
    
    val widget = remember { 
        JediTermWidget(settings).apply {
            // Style the scrollbar to match dark theme
            styleScrollBar(this)
        }
    }

    DisposableEffect(containerId) {
        // Build docker context name
        val context = if (profileName == null || profileName == "default") "colima" else "colima-$profileName"
        
        // Resolve docker executable path for packaged app
        val dockerPath = resolveExecutable("docker")
        
        // Build the command - wrapping with echo to show what's being executed
        val shortId = containerId.take(12)
        
        // The actual command with shell detection
        val shellScript = """
            echo -e "\033[1;34m▶ Connecting to container: $shortId\033[0m"
            echo -e "\033[0;90m  Command: docker --context $context exec -it $shortId <shell>\033[0m"
            echo ""
            if [ -x /bin/bash ]; then exec /bin/bash; else exec /bin/sh; fi
        """.trimIndent()
        
        val command = arrayOf(
            dockerPath, "--context", context, "exec", "-it", containerId, 
            "/bin/sh", "-c", shellScript
        )
        
        // Fix for GUI apps not loading .zshrc/.bash_profile PATH
        val env = System.getenv().toMutableMap()
        env["TERM"] = "xterm-256color"
        val existingPath = env["PATH"] ?: ""
        val commonPaths = listOf(
            "/usr/local/bin",
            "/opt/homebrew/bin",
            "/opt/local/bin",
            System.getProperty("user.home") + "/.colima/bin",
            System.getProperty("user.home") + "/.docker/bin"
        ).joinToString(":")
        env["PATH"] = if (existingPath.isNotEmpty()) "$commonPaths:$existingPath" else commonPaths

        var connector: PtyProcessTtyConnector? = null
        var ptyProcess: com.pty4j.PtyProcess? = null

        try {
            ptyProcess = PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(env)
                .start()

            connector = PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8)
            widget.setTtyConnector(connector)
            widget.start()
            
            // Request focus for keyboard input
            widget.requestFocusInWindow()
            
            onTerminalSessionActive(true)

        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app, we should show this error in the terminal
        }

        onDispose {
            try {
                // Close the connector first
                connector?.close()
                
                // Explicitly destroy the PTY process to ensure cleanup
                ptyProcess?.let { process ->
                    if (process.isAlive) {
                        process.destroyForcibly()
                        // Wait briefly for process to terminate
                        process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS)
                    }
                }
                
                widget.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onTerminalSessionActive(false)
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = { widget }
    )
}

/**
 * Apply dark theme styling to the terminal's scrollbar.
 */
private fun styleScrollBar(widget: JediTermWidget) {
    // Find and style all scrollbars in the widget hierarchy
    fun findScrollBars(component: java.awt.Component): List<JScrollBar> {
        val scrollBars = mutableListOf<JScrollBar>()
        if (component is JScrollBar) {
            scrollBars.add(component)
        }
        if (component is java.awt.Container) {
            component.components.forEach { child ->
                scrollBars.addAll(findScrollBars(child))
            }
        }
        return scrollBars
    }
    
    val darkTrackColor = Color(0x1E, 0x1E, 0x1E)     // surface: #1E1E1E
    val darkThumbColor = Color(0x45, 0x45, 0x45)     // Subtle grey thumb
    val darkThumbHover = Color(0x60, 0x60, 0x60)     // Slightly lighter on hover
    
    findScrollBars(widget).forEach { scrollBar ->
        scrollBar.setUI(object : BasicScrollBarUI() {
            override fun configureScrollBarColors() {
                thumbColor = darkThumbColor
                thumbHighlightColor = darkThumbHover
                thumbDarkShadowColor = darkThumbColor
                thumbLightShadowColor = darkThumbColor
                trackColor = darkTrackColor
                trackHighlightColor = darkTrackColor
            }
            
            // Remove the arrow buttons for a cleaner look
            override fun createDecreaseButton(orientation: Int) = createZeroButton()
            override fun createIncreaseButton(orientation: Int) = createZeroButton()
            
            private fun createZeroButton() = javax.swing.JButton().apply {
                preferredSize = java.awt.Dimension(0, 0)
                minimumSize = java.awt.Dimension(0, 0)
                maximumSize = java.awt.Dimension(0, 0)
            }
        })
        scrollBar.background = darkTrackColor
    }
}

/**
 * Resolve executable path for packaged apps that don't inherit shell PATH.
 */
private fun resolveExecutable(command: String): String {
    if (command.contains("/")) return command // Already absolute or relative path
    
    val searchPaths = listOf(
        "/usr/local/bin",
        "/opt/homebrew/bin",
        "/opt/local/bin",
        System.getProperty("user.home") + "/.colima/bin",
        System.getProperty("user.home") + "/.docker/bin",
        "/usr/bin",
        "/bin",
        "/usr/sbin",
        "/sbin"
    )
    
    for (path in searchPaths) {
        val file = java.io.File(path, command)
        if (file.exists() && file.canExecute()) {
            return file.absolutePath
        }
    }
    
    return command // Fallback to system lookup
}
