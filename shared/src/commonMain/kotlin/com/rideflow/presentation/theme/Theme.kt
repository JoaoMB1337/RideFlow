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

// Paleta desportiva: verde "lima" (potência / ação principal), azul (velocidade),
// coral-vermelho (frequência cardíaca). Contrastes verificados para texto sobre os containers.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3DED8A),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF00532A),
    onPrimaryContainer = Color(0xFF8CFFB3),
    secondary = Color(0xFF9CC2FF),
    onSecondary = Color(0xFF003064),
    secondaryContainer = Color(0xFF00468C),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFFFFB4A6),
    onTertiary = Color(0xFF65150B),
    tertiaryContainer = Color(0xFF8A2A1D),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF0F1411),
    onBackground = Color(0xFFDFE4DE),
    surface = Color(0xFF0F1411),
    onSurface = Color(0xFFDFE4DE),
    surfaceVariant = Color(0xFF404941),
    onSurfaceVariant = Color(0xFFC0C9BF),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF262B27),
    surfaceContainerHighest = Color(0xFF303632),
    outline = Color(0xFF8A938A),
    outlineVariant = Color(0xFF404941),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D38),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9AF6B6),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF1F5FA8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001B3E),
    tertiary = Color(0xFFB3261E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF410001),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF404941),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEAEFE9),
    surfaceContainerHigh = Color(0xFFE4EAE3),
    surfaceContainerHighest = Color(0xFFDFE4DD),
    outline = Color(0xFF707970),
    outlineVariant = Color(0xFFC0C9BF),
)

// Algarismos tabulares nos estilos "display" para os números não "saltarem" a cada segundo.
private fun TextStyle.numeric() = copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Black)

private val RideFlowTypography = Typography().run {
    copy(
        displayLarge = displayLarge.numeric(),
        displayMedium = displayMedium.numeric(),
        displaySmall = displaySmall.numeric(),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold),
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
