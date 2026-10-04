package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NovaCyanLight,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFA6EEFF),
    secondary = NovaVioletAccent,
    onSecondary = Color(0xFF1E2678),
    secondaryContainer = Color(0xFF333D96),
    onSecondaryContainer = Color(0xFFE0E0FF),
    tertiary = NovaEmerald,
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF005237),
    onTertiaryContainer = Color(0xFF72F8B6),
    background = NovaBackgroundDark,
    onBackground = Color(0xFFE2E8F0),
    surface = NovaSurfaceDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = NovaSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = NovaBorderDark,
    error = NovaRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NovaCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6EEFF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF4C58B3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF070F68),
    tertiary = Color(0xFF006D4A),
    onTertiary = Color.White,
    background = NovaBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = NovaSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = NovaSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = NovaBorderLight,
    error = NovaRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to IDE dark mode for code tools
    dynamicColor: Boolean = false, // Keep distinctive cyber IDE look by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
