package com.colimator.app.model

/**
 * Represents a single line of log output.
 */
data class LogLine(
    val timestamp: String,
    val content: String,
    val id: Long = nextId()
) {
    companion object {
        private var counter = 0L
        @Synchronized
        fun nextId(): Long = counter++
    }
}
