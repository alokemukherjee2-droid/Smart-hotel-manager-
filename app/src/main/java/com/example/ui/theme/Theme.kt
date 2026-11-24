package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightHotelColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = NavySoft,
    onPrimaryContainer = NavyOnContainer,
    secondary = GoldPrimary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = TealHeritage,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealOnContainer,
    background = SurfaceCanvas,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = Color(0xFFE7ECF4),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEECEE),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkHotelColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = NavyDeep,
    primaryContainer = NavyRich,
    onPrimaryContainer = Color.White,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF2E1E00),
    secondaryContainer = Color(0xFF473107),
    onSecondaryContainer = GoldContainer,
    tertiary = Color(0xFF6CD2CC),
    onTertiary = Color(0xFF003735),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF2B3E5E),
    outlineVariant = Color(0xFF1F304D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val HotelShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartHotelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkHotelColorScheme else LightHotelColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HotelShapes,
        content = content
    )
}
