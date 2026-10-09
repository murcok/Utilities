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
    primary = Cyan80,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004F5C),
    onPrimaryContainer = Color(0xFFA5EEFC),
    secondary = Amber80,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF623F00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Emerald80,
    onTertiary = Color(0xFF003823),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = Cyan40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5EEFC),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Emerald40,
    onTertiary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun SoundFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false by default to showcase our curated audio-focus aesthetic
    content: @Composable () -> Unit
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
