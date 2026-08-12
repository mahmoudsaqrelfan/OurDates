package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = MedicalSurface,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary = CyanAccent,
    onSecondary = MedicalSurface,
    background = MedicalBackground,
    onBackground = TextPrimary,
    surface = MedicalSurface,
    onSurface = TextPrimary,
    surfaceVariant = PastelCyanCard,
    onSurfaceVariant = TextSecondary,
    outline = PastelCyanBorder
)

@Composable
fun MawaeednaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Keep backwards-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    MawaeednaTheme(darkTheme = darkTheme, content = content)
}
