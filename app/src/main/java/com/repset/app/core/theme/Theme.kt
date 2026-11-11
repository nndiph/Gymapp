package com.repset.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App theme. Follows the system light/dark setting.
 * Dynamic (wallpaper) colour is intentionally off so the brand looks the same on every device.
 */
@Composable
fun RepsetTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = RepsetTypography,
        content = content
    )
}
