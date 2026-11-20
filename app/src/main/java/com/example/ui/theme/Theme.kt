package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val UltraWrapDarkColorScheme = darkColorScheme(
    primary = MetallicGold,
    onPrimary = ObsidianBlack,
    primaryContainer = GoldTintSurface,
    onPrimaryContainer = LightGold,
    secondary = RichGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = IvoryText,
    tertiary = LightGold,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = MutedSilver,
    outline = ObsidianOutline,
    outlineVariant = ObsidianElevated,
    error = CrimsonError,
    onError = PureWhite,
    errorContainer = CrimsonSurface,
    onErrorContainer = CrimsonError
)

val UltraWrapShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun UltraWrapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UltraWrapDarkColorScheme,
        typography = UltraWrapTypography,
        shapes = UltraWrapShapes,
        content = content
    )
}
