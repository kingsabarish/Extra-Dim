package com.extradim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D3A6E),
    secondary = Color(0xFF5B5796),
    background = Color(0xFFFAF9FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8B4FF),
    secondary = Color(0xFF9A96D8),
    background = Color(0xFF1A1831),
)

/**
 * Extra Dim app theme. Uses a static Material 3 scheme; dynamic color is
 * intentionally skipped to keep the dimming feature visually stable. On API
 * 31+ the system theme (light/dark) is still honored.
 */
@Composable
fun ExtraDimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
