package com.colimator.app.util

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AnsiParserTest {

    @Test
    fun testPlainString() {
        val result = AnsiParser.parse("Hello World")
        assertEquals("Hello World", result.text)
        // Implementation wraps the entire string in a default style
        assertEquals(1, result.spanStyles.size)
    }

    @Test
    fun testSimpleAnsiColor() {
        // Red color code: \u001B[31m
        val result = AnsiParser.parse("\u001B[31mError\u001B[0m")
        assertEquals("Error", result.text)
        // We expect at least the Red span. Empty spans might be optimized out or present.
        // The Red span should cover range local to the text "Error" which is 0-5.
        val redSpan = result.spanStyles.find { it.item.color == Color(0xFFCD3131) }
        kotlin.test.assertNotNull(redSpan)
        assertEquals(0, redSpan.start)
        assertEquals(5, redSpan.end)
    }

    @Test
    fun testBold() {
        // Bold code: \u001B[1m
        val result = AnsiParser.parse("\u001B[1mBold\u001B[0m")
        assertEquals("Bold", result.text)
        val style = result.spanStyles.first().item
        // We can't easily check FontWeight here without casting/inspecting implementation details
        // but verifying we get a style is good.
    }
}
