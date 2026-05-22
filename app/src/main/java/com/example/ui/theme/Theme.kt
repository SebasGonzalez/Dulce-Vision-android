package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ImmersiveDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = CyberPurple,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    error = Color(0xFFCF6679),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    outline = CardBorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We force rich cinematic dark theme
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain strong cyber cinematic brand styling
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ImmersiveDarkColorScheme,
        typography = Typography,
        content = content
    )
}
