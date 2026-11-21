package com.example.ui.theme

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeMode

val LocalLithfitReducedMotion = staticCompositionLocalOf { false }

private val LithfitLightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = WarmCream,
    primaryContainer = SoftStone,
    onPrimaryContainer = InkBlack,
    secondary = Charcoal,
    onSecondary = WarmCream,
    secondaryContainer = SoftGrey.copy(alpha = 0.5f),
    onSecondaryContainer = InkBlack,
    tertiary = Charcoal,
    onTertiary = PureWhite,
    tertiaryContainer = PureWhite,
    onTertiaryContainer = InkBlack,
    background = WarmCream,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftStone,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = WarmCream,
    surfaceContainer = SoftStone,
    surfaceContainerHigh = Color(0xFFE5DFD4),
    surfaceContainerHighest = SoftGrey,
    outline = SoftGrey,
    outlineVariant = Color(0xFFE3DED3),
    error = CrimsonError,
    onError = PureWhite
)

private val LithfitDarkColorScheme = darkColorScheme(
    primary = WarmCream,
    onPrimary = InkBlack,
    primaryContainer = CharcoalElevated,
    onPrimaryContainer = WarmCream,
    secondary = TaupeGrey,
    onSecondary = InkBlack,
    secondaryContainer = Color(0xFF2F2D2A),
    onSecondaryContainer = WarmCream,
    tertiary = PureWhite,
    onTertiary = InkBlack,
    tertiaryContainer = CharcoalElevated,
    onTertiaryContainer = WarmCream,
    background = ObsidianDark,
    onBackground = WarmCream,
    surface = CharcoalSurface,
    onSurface = WarmCream,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = TaupeGrey,
    surfaceContainerLowest = ObsidianDark,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalElevated,
    surfaceContainerHigh = Color(0xFF2A2926),
    surfaceContainerHighest = Color(0xFF33312D),
    outline = HairlineDark,
    outlineVariant = Color(0xFF282623),
    error = CrimsonErrorDark,
    onError = ObsidianDark
)

val LithfitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LithfitTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    largeReadableText: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val colorScheme = if (useDark) LithfitDarkColorScheme else LithfitLightColorScheme
    val typography = remember(largeReadableText) { buildLithfitTypography(largeReadableText) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDark
                    isAppearanceLightNavigationBars = !useDark
                }
            }
        }
    }

    CompositionLocalProvider(LocalLithfitReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = LithfitShapes,
            content = content
        )
    }
}
