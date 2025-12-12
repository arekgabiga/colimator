package com.colimator.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Parses ANSI escape codes into an AnnotatedString.
 */
object AnsiParser {
    
    // Regex to match ANSI escape codes: \u001B\[[parameter]m
    private val ansiRegex = Regex("\u001B\\[[;\\d]*m")

    fun parse(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            var currentColor = Color.Unspecified
            var currentBackground = Color.Unspecified
            var isBold = false
            
            // Re-apply styles function
            fun pushCurrentStyles() {
                val style = SpanStyle(
                    color = currentColor,
                    background = currentBackground,
                    fontWeight = if (isBold) FontWeight.Bold else null
                )
                pushStyle(style)
            }

            // Initially push default style
            pushCurrentStyles()

            ansiRegex.findAll(text).forEach { match ->
                // Append text before the match
                if (match.range.first > currentIndex) {
                    append(text.substring(currentIndex, match.range.first))
                }
                
                // Process codes
                val content = match.value
                // Remove \u001B[ and m, then split by ;
                val codes = content.drop(2).dropLast(1).split(";").mapNotNull { it.toIntOrNull() }
                
                // If codes is empty, it's usually reset (0)
                val effectiveCodes = if (codes.isEmpty()) listOf(0) else codes
                
                // Pop previous style to apply new one
                pop()
                
                effectiveCodes.forEach { code ->
                    when (code) {
                        0 -> { // Reset
                            currentColor = Color.Unspecified
                            currentBackground = Color.Unspecified
                            isBold = false
                        }
                        1 -> isBold = true
                        22 -> isBold = false // Normal intensity
                        in 30..37 -> currentColor = getAnsiColor(code - 30)
                        39 -> currentColor = Color.Unspecified // Default text color
                        in 40..47 -> currentBackground = getAnsiColor(code - 40)
                        49 -> currentBackground = Color.Unspecified // Default background
                        in 90..97 -> currentColor = getAnsiBrightColor(code - 90)
                        in 100..107 -> currentBackground = getAnsiBrightColor(code - 100)
                    }
                }
                
                // Push new style
                pushCurrentStyles()
                
                currentIndex = match.range.last + 1
            }
            
            // Append remaining text
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
            
            // Pop final style
            pop()
        }
    }
    
    private fun getAnsiColor(index: Int): Color {
        return when (index) {
            0 -> Color.Black
            1 -> Color(0xFFCD3131) // Red
            2 -> Color(0xFF0DBC79) // Green
            3 -> Color(0xFFE5E510) // Yellow
            4 -> Color(0xFF2472C8) // Blue
            5 -> Color(0xFFBC3FBC) // Magenta
            6 -> Color(0xFF11A8CD) // Cyan
            7 -> Color(0xFFE5E5E5) // White
            else -> Color.Unspecified
        }
    }
    
    private fun getAnsiBrightColor(index: Int): Color {
        return when (index) {
            0 -> Color(0xFF666666) // Bright Black (Gray)
            1 -> Color(0xFFF14C4C) // Bright Red
            2 -> Color(0xFF23D18B) // Bright Green
            3 -> Color(0xFFF5F543) // Bright Yellow
            4 -> Color(0xFF3B8EEA) // Bright Blue
            5 -> Color(0xFFD670D6) // Bright Magenta
            6 -> Color(0xFF29B8DB) // Bright Cyan
            7 -> Color(0xFFFFFFFF) // Bright White
            else -> Color.Unspecified
        }
    }
}
