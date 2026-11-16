package com.rideflow.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

// Superfícies neutras (sem tinta colorida) e um único acento. As cores das métricas
// (HR = tertiary, velocidade = secondary) só aparecem nos números e nos gráficos.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9A52),
    onPrimary = Color(0xFF4A1A00),
    primaryContainer = Color(0xFF5A2A0A),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFF8DB8F0),
    onSecondary = Color(0xFF00315C),
    secondaryContainer = Color(0xFF2A2F36),
    onSecondaryContainer = Color(0xFFDCE3EE),
    tertiary = Color(0xFFF08A7A),
    onTertiary = Color(0xFF5C130A),
    tertiaryContainer = Color(0xFF3A2422),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA6A6A6),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2B2B2B),
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFF333333),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC4500F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF341000),
    secondary = Color(0xFF2B5C99),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E8EE),
    onSecondaryContainer = Color(0xFF1A1F26),
    tertiary = Color(0xFFB3261E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E3E0),
    onTertiaryContainer = Color(0xFF410001),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE4E4E4),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFD6D6D6),
)

// Algarismos tabulares nos estilos "display" para os números não "saltarem" a cada segundo.
private fun TextStyle.numeric() = copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

private val RideFlowTypography = Typography().run {
    copy(
        displayLarge = displayLarge.numeric(),
        displayMedium = displayMedium.numeric(),
        displaySmall = displaySmall.numeric(),
    )
}

@Composable
fun RideFlowTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RideFlowTypography,
        content = content
    )
}
