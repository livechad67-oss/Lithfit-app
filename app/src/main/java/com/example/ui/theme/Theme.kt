package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeMode

private val LithfitLightColorScheme = lightColorScheme(
    primary = DeepCharcoal,
    onPrimary = WarmAlabaster,
    primaryContainer = SoftStone,
    onPrimaryContainer = DeepCharcoal,
    secondary = MutedSlate,
    onSecondary = ElevatedPaper,
    secondaryContainer = WarmSand,
    onSecondaryContainer = DeepCharcoal,
    tertiary = ElectricLimeMuted,
    onTertiary = OnElectricLime,
    tertiaryContainer = ElectricLime,
    onTertiaryContainer = OnElectricLime,
    background = WarmAlabaster,
    onBackground = InkCharcoal,
    surface = ElevatedPaper,
    onSurface = InkCharcoal,
    surfaceVariant = SoftStone,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = ElevatedPaper,
    surfaceContainerLow = WarmAlabaster,
    surfaceContainer = SoftStone,
    surfaceContainerHigh = Color(0xFFEEECE7),
    surfaceContainerHighest = Color(0xFFE6E4DD),
    outline = HairlineLight,
    outlineVariant = Color(0xFFEEECE5),
    error = CrimsonError,
    onError = Color.White
)

private val LithfitDarkColorScheme = darkColorScheme(
    primary = WarmAlabaster,
    onPrimary = ObsidianDark,
    primaryContainer = CharcoalElevated,
    onPrimaryContainer = WarmAlabaster,
    secondary = SoftSilver,
    onSecondary = ObsidianDark,
    secondaryContainer = Color(0xFF2B2B31),
    onSecondaryContainer = WarmAlabaster,
    tertiary = ElectricLime,
    onTertiary = OnElectricLime,
    tertiaryContainer = Color(0xFF333B14),
    onTertiaryContainer = ElectricLime,
    background = ObsidianDark,
    onBackground = WarmAlabaster,
    surface = CharcoalSurface,
    onSurface = WarmAlabaster,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = SoftSilver,
    surfaceContainerLowest = ObsidianDark,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalElevated,
    surfaceContainerHigh = Color(0xFF2A2A30),
    surfaceContainerHighest = Color(0xFF323239),
    outline = HairlineDark,
    outlineVariant = Color(0xFF242429),
    error = CrimsonErrorDark,
    onError = ObsidianDark
)

val LithfitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun LithfitTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val colorScheme = if (useDark) LithfitDarkColorScheme else LithfitLightColorScheme

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LithfitTypography,
        shapes = LithfitShapes,
        content = content
    )
}
