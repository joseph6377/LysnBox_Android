package com.example.lysnboxandroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = J7AccentDark,
    onPrimary = J7AppBackgroundDark,
    secondary = J7TextSecondaryDark,
    background = J7AppBackgroundDark,
    onBackground = J7TextPrimaryDark,
    surface = J7SurfaceDark,
    onSurface = J7TextPrimaryDark,
    surfaceVariant = J7SurfaceDark,
    outline = J7BorderDark,
)

private val LightColorScheme = lightColorScheme(
    primary = J7Accent,
    onPrimary = J7Surface,
    secondary = J7TextSecondary,
    background = J7AppBackground,
    onBackground = J7TextPrimary,
    surface = J7Surface,
    onSurface = J7TextPrimary,
    surfaceVariant = J7Surface,
    outline = J7Border,
)

// Continuous-style rounded corners matching iOS.
private val LysnShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LysnBoxAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    readingPalette: ThemePalette = ThemePalette.ModernBlue,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalPalette provides readingPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = LysnShapes,
            content = content
        )
    }
}
