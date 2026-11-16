package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.AppPalette

val LocalBaonDarkTheme = compositionLocalOf { false }
val LocalBaonAppPalette = compositionLocalOf { AppPalette.MINT }

private val MintDarkColorScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = MintOnPrimaryDark,
    primaryContainer = MintPrimaryContainerDark,
    onPrimaryContainer = MintOnPrimaryContainerDark,
    secondary = MintSecondaryDark,
    onSecondary = MintOnSecondaryDark,
    secondaryContainer = MintSecondaryContainerDark,
    onSecondaryContainer = MintOnSecondaryContainerDark,
    tertiary = MintPrimaryDark,
    background = MintBackgroundDark,
    onBackground = IceInkDark,
    surface = MintSurfaceDark,
    onSurface = IceInkDark,
    surfaceVariant = MintSurfaceVariantDark,
    onSurfaceVariant = IceMutedDark,
    outline = MintOutlineDark,
    outlineVariant = MintOutlineDark
)

private val MintLightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = MintOnSecondary,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = MintOnSecondaryContainer,
    tertiary = MintTertiary,
    onTertiary = MintOnTertiary,
    tertiaryContainer = MintTertiaryContainer,
    onTertiaryContainer = MintOnTertiaryContainer,
    background = MintBackgroundLight,
    onBackground = SlateInkLight,
    surface = MintSurfaceLight,
    onSurface = SlateInkLight,
    surfaceVariant = MintSurfaceVariantLight,
    onSurfaceVariant = SlateMutedLight,
    outline = MintOutlineLight,
    outlineVariant = MintOutlineLight
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkPrimaryDark,
    onPrimary = PinkOnPrimaryDark,
    primaryContainer = PinkPrimaryContainerDark,
    onPrimaryContainer = PinkOnPrimaryContainerDark,
    secondary = PinkSecondaryDark,
    onSecondary = PinkOnSecondaryDark,
    secondaryContainer = PinkSecondaryContainerDark,
    onSecondaryContainer = PinkOnSecondaryContainerDark,
    tertiary = PinkPrimaryDark,
    background = PinkBackgroundDark,
    onBackground = PinkInkDark,
    surface = PinkSurfaceDark,
    onSurface = PinkInkDark,
    surfaceVariant = PinkSurfaceVariantDark,
    onSurfaceVariant = PinkMutedDark,
    outline = PinkOutlineDark,
    outlineVariant = PinkOutlineDark
)

private val PinkLightColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = PinkOnPrimary,
    primaryContainer = PinkPrimaryContainer,
    onPrimaryContainer = PinkOnPrimaryContainer,
    secondary = PinkSecondary,
    onSecondary = PinkOnSecondary,
    secondaryContainer = PinkSecondaryContainer,
    onSecondaryContainer = PinkOnSecondaryContainer,
    tertiary = PinkTertiary,
    onTertiary = PinkOnTertiary,
    tertiaryContainer = PinkTertiaryContainer,
    onTertiaryContainer = PinkOnTertiaryContainer,
    background = PinkBackgroundLight,
    onBackground = PinkInkLight,
    surface = PinkSurfaceLight,
    onSurface = PinkInkLight,
    surfaceVariant = PinkSurfaceVariantLight,
    onSurfaceVariant = PinkMutedLight,
    outline = PinkOutlineLight,
    outlineVariant = PinkOutlineLight
)

val BaonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BaonTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appPalette: AppPalette = AppPalette.MINT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        appPalette == AppPalette.PINK_PASTEL -> {
            if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
        }
        else -> {
            if (darkTheme) MintDarkColorScheme else MintLightColorScheme
        }
    }

    CompositionLocalProvider(
        LocalBaonDarkTheme provides darkTheme,
        LocalBaonAppPalette provides appPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = BaonShapes,
            content = content
        )
    }
}
