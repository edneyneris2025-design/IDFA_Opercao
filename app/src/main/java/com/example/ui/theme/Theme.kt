package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalColorScheme = darkColorScheme(
    primary = TacticalGreen,
    onPrimary = Color.Black,
    primaryContainer = TacticalGreenDim,
    onPrimaryContainer = Color.White,
    secondary = TacticalCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = TacticalCyan,
    tertiary = TacticalAmber,
    onTertiary = Color.Black,
    error = TacticalRed,
    onError = Color.White,
    background = BallisticBlack,
    onBackground = TextPrimary,
    surface = TacticalCardDark,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    outlineVariant = Color(0xFF1B2B23)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography,
        content = content
    )
}
