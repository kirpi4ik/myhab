package org.myhab.voice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E7D32)
private val GreenDark = Color(0xFF1B5E20)
private val GreenLight = Color(0xFF60AD5E)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = GreenLight,
    tertiary = GreenDark
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Green,
    tertiary = GreenDark
)

@Composable
fun MyHabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
