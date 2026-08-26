package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PyMobileDarkColorScheme = darkColorScheme(
    primary = IdeAccentBlue,
    onPrimary = Color.White,
    primaryContainer = IdeDarkSurfaceVariant,
    onPrimaryContainer = IdeTextPrimary,
    secondary = IdePythonBlue,
    onSecondary = Color.White,
    secondaryContainer = IdeDarkSurfaceVariant,
    onSecondaryContainer = IdeTextPrimary,
    tertiary = IdePythonYellow,
    onTertiary = Color.Black,
    tertiaryContainer = IdeDarkSurfaceVariant,
    onTertiaryContainer = IdeTextPrimary,
    background = IdeDarkBackground,
    onBackground = IdeTextPrimary,
    surface = IdeDarkSurface,
    onSurface = IdeTextPrimary,
    surfaceVariant = IdeDarkSurfaceVariant,
    onSurfaceVariant = IdeTextSecondary,
    outline = IdeDarkBorder,
    outlineVariant = IdeDarkSurfaceVariant,
    error = IdeRed,
    onError = Color.White
)

@Composable
fun PyMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PyMobileDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    PyMobileTheme(content = content)
}
