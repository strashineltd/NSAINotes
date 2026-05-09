package com.nsai.notes.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nsai.notes.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = BlueGrey40,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    outline = Color(0xFFDADCE0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = BlueGrey80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2B2930),
    surfaceVariant = Color(0xFF3C3A41),
    onPrimary = OnSurfaceLight,
    onSecondary = OnSurfaceLight,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    outline = Color(0xFF49454F)
)

@Composable
fun NSAINotesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: Float = 1.0f,
    animationTokens: AnimationTokens = AnimationTokens.FULL,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val context = LocalContext.current
    // Cache the color scheme to avoid recomputing dynamic colors (Material You) on every
    // recomposition — dynamicDarkColorScheme reads system wallpaper colors and is expensive.
    val colorScheme = remember(isDark) {
        when {
            Build.VERSION.SDK_INT >= 31 -> {
                if (isDark) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            isDark -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    val scaledTypography = remember(fontScale) {
        if (fontScale == 1.0f) NSAITypography
        else Typography(
            displayLarge = NSAITypography.displayLarge.copy(fontSize = (28 * fontScale).sp, lineHeight = (36 * fontScale).sp),
            headlineMedium = NSAITypography.headlineMedium.copy(fontSize = (22 * fontScale).sp, lineHeight = (28 * fontScale).sp),
            titleLarge = NSAITypography.titleLarge.copy(fontSize = (18 * fontScale).sp, lineHeight = (24 * fontScale).sp),
            titleMedium = NSAITypography.titleMedium.copy(fontSize = (16 * fontScale).sp, lineHeight = (22 * fontScale).sp),
            bodyLarge = NSAITypography.bodyLarge.copy(fontSize = (16 * fontScale).sp, lineHeight = (24 * fontScale).sp),
            bodyMedium = NSAITypography.bodyMedium.copy(fontSize = (14 * fontScale).sp, lineHeight = (20 * fontScale).sp),
            labelMedium = NSAITypography.labelMedium.copy(fontSize = (12 * fontScale).sp, lineHeight = (16 * fontScale).sp)
        )
    }

    CompositionLocalProvider(LocalAnimationConfig provides animationTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}
