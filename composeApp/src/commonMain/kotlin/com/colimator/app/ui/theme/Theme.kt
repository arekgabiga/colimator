package com.colimator.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colimator dark color scheme - developer-optimized with high contrast.
 */
private val ColimatorDarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),           // Soft blue - main accent
    onPrimary = Color(0xFF003258),          // Dark blue on primary
    primaryContainer = Color(0xFF004880),   // Slightly lighter container
    onPrimaryContainer = Color(0xFFD1E4FF), // Light blue text on container
    
    secondary = Color(0xFFB0BEC5),          // Blue grey
    onSecondary = Color(0xFF263238),        // Dark slate
    secondaryContainer = Color(0xFF455A64), // Medium slate
    onSecondaryContainer = Color(0xFFECEFF1),
    
    tertiary = Color(0xFF80CBC4),           // Teal accent
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004D56),
    onTertiaryContainer = Color(0xFFA7F3EC),
    
    error = Color(0xFFFFB4AB),              // Muted red
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF121212),         // Near-black background
    onBackground = Color(0xFFE3E3E3),       // Light grey text
    
    surface = Color(0xFF1E1E1E),            // Dark grey surface
    onSurface = Color(0xFFE3E3E3),          // Light grey text
    
    surfaceVariant = Color(0xFF2D2D2D),     // Slightly lighter surface
    onSurfaceVariant = Color(0xFFCAC4D0),   // Muted text
    
    outline = Color(0xFF938F99),            // Border color
    outlineVariant = Color(0xFF49454F),     // Subtle borders
    
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF1565C0),
)

/**
 * Colimator app theme - always dark for developer experience.
 */
@Composable
fun ColimatorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColimatorDarkColors,
        content = content
    )
}
