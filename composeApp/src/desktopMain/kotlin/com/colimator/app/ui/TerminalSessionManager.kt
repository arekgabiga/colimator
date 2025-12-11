package com.colimator.app.ui

import com.jediterm.pty.PtyProcessTtyConnector
import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import java.awt.Color
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.swing.JScrollBar
import javax.swing.plaf.basic.BasicScrollBarUI

/**
 * Represents an active terminal session.
 */
data class TerminalSession(
    val widget: JediTermWidget,
    val ptyProcess: PtyProcess,
    val connector: PtyProcessTtyConnector,
    val containerId: String,
    val profileName: String?
) {
    val isAlive: Boolean
        get() = ptyProcess.isAlive
}

/**
 * Manages terminal sessions, caching them by container ID to persist across tab switches.
 */
object TerminalSessionManager {
    
    private val sessions = ConcurrentHashMap<String, TerminalSession>()
    
    /**
     * Get an existing session or create a new one for the given container.
     */
    fun getOrCreateSession(
        containerId: String,
        profileName: String?,
        onSessionCreated: () -> Unit
    ): TerminalSession {
        // Check for existing alive session
        sessions[containerId]?.let { session ->
            if (session.isAlive) {
                // Just request focus for the existing widget
                session.widget.requestFocusInWindow()
                return session
            } else {
                // Session died, clean it up
                closeSession(containerId)
            }
        }
        
        // Create new session
        val session = createSession(containerId, profileName)
        sessions[containerId] = session
        onSessionCreated()
        return session
    }
    
    /**
     * Check if a session exists and is alive.
     */
    fun hasActiveSession(containerId: String): Boolean {
        return sessions[containerId]?.isAlive == true
    }
    
    /**
     * Close a specific session.
     */
    fun closeSession(containerId: String) {
        sessions.remove(containerId)?.let { session ->
            try {
                session.connector.close()
                if (session.ptyProcess.isAlive) {
                    session.ptyProcess.destroyForcibly()
                    session.ptyProcess.waitFor(1, TimeUnit.SECONDS)
                }
                session.widget.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Close all sessions. Call this when the app is shutting down.
     */
    fun closeAllSessions() {
        sessions.keys.toList().forEach { closeSession(it) }
    }
    
    private fun createSession(containerId: String, profileName: String?): TerminalSession {
        val settings = object : DefaultSettingsProvider() {
            override fun getDefaultStyle(): TextStyle {
                return TextStyle(
                    TerminalColor.rgb(227, 227, 227),
                    TerminalColor.rgb(18, 18, 18)
                )
            }
            
            override fun getSelectionColor(): TextStyle {
                return TextStyle(
                    TerminalColor.WHITE,
                    TerminalColor.rgb(0, 72, 128)
                )
            }
            
            override fun getTerminalFont(): java.awt.Font {
                return java.awt.Font("Menlo", java.awt.Font.PLAIN, 13)
            }
        }
        
        val widget = JediTermWidget(settings).apply {
            styleScrollBar(this)
        }
        
        val context = if (profileName == null || profileName == "default") "colima" else "colima-$profileName"
        val dockerPath = resolveExecutable("docker")
        val shortId = containerId.take(12)
        
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
        
        val ptyProcess = PtyProcessBuilder()
            .setCommand(command)
            .setEnvironment(env)
            .start()
        
        val connector = PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8)
        widget.setTtyConnector(connector)
        widget.start()
        widget.requestFocusInWindow()
        
        return TerminalSession(widget, ptyProcess, connector, containerId, profileName)
    }
    
    private fun styleScrollBar(widget: JediTermWidget) {
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
        
        val darkTrackColor = Color(0x1E, 0x1E, 0x1E)
        val darkThumbColor = Color(0x45, 0x45, 0x45)
        val darkThumbHover = Color(0x60, 0x60, 0x60)
        
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
    
    private fun resolveExecutable(command: String): String {
        if (command.contains("/")) return command
        
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
        
        return command
    }
}
