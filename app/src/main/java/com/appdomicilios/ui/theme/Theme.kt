package com.appdomicilios.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Papaya,
    onPrimary = Cream,
    primaryContainer = PapayaSoft,
    onPrimaryContainer = Coffee,
    secondary = Emerald,
    onSecondary = Cream,
    secondaryContainer = EmeraldSoft,
    onSecondaryContainer = Coffee,
    tertiary = Guava,
    onTertiary = Cream,
    tertiaryContainer = GuavaSoft,
    onTertiaryContainer = Coffee,
    errorContainer = Color(0xFFFFDAD6),
    background = Cream,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Coffee,
    outline = Color(0xFFB58C6B),
)

private val DarkColors = darkColorScheme(
    primary = Marigold,
    onPrimary = Night,
    primaryContainer = Color(0xFF6A3117),
    onPrimaryContainer = Color(0xFFFFD9C7),
    secondary = EmeraldSoft,
    onSecondary = Night,
    secondaryContainer = Color(0xFF174433),
    onSecondaryContainer = Color(0xFFC8EEDB),
    tertiary = Color(0xFFFFB3C0),
    onTertiary = Night,
    tertiaryContainer = Color(0xFF6A3240),
    onTertiaryContainer = Color(0xFFFFD9DE),
    background = Night,
    onBackground = Cream,
    surface = NightCard,
    onSurface = Cream,
    surfaceVariant = NightSoft,
    onSurfaceVariant = Sand,
    outline = Color(0xFFD1B49A),
)

@Composable
fun AppDomiciliosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
