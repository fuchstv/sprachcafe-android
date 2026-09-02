package org.sprachcafe.team.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SprachCafeRed,
    onPrimary = Color.White,
    primaryContainer = SprachCafeCream,
    onPrimaryContainer = SprachCafeDarkRed,
    secondary = AccentAmber,
    onSecondary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = SprachCafeBorder
)

@Composable
fun SprachCafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
