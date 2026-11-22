package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = IndigoPrimary,
    secondary = SaffronAccent,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = Color(0xFF4A1800),
    tertiary = EmeraldFree,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldFreeContainer,
    onTertiaryContainer = Color(0xFF022C22),
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF283593),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = SaffronAccentDark,
    onSecondary = Color(0xFF3E1800),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

@Composable
fun TargetBootcampTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TargetBootcampTypography,
        content = content
    )
}
