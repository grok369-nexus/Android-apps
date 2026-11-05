package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccentLight,
    onPrimary = Color(0xFF203027),
    primaryContainer = CyanAccentDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldEnjoyment,
    onSecondary = Color.White,
    secondaryContainer = SlateNavy,
    onSecondaryContainer = Color.White,
    tertiary = AmberDining,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF657367)
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccentDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8D9),
    onPrimaryContainer = Color(0xFF294332),
    secondary = EmeraldEnjoyment,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EBDD),
    onSecondaryContainer = Color(0xFF344A39),
    tertiary = AmberDining,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD5CCBE)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
