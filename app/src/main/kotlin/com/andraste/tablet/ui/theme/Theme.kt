package com.andraste.tablet.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IceniColorScheme = darkColorScheme(
    primary              = IceniGold,
    onPrimary            = Color(0xFF1A1400),
    primaryContainer     = Color(0xFF3D2E00),
    onPrimaryContainer   = IceniGold,
    secondary            = IceniTeal,
    onSecondary          = Color(0xFF001F1D),
    secondaryContainer   = IceniTealDark,
    onSecondaryContainer = IceniText,
    tertiary             = IceniBlue,
    background           = IceniDeepGreen,
    onBackground         = IceniText,
    surface              = IceniGreen,
    onSurface            = IceniText,
    surfaceVariant       = IceniGreenLight,
    onSurfaceVariant     = IceniTextMuted,
    outline              = IceniGoldDim,
    error                = IceniRed,
    onError              = Color(0xFF000000),
)

@Composable
fun AndrasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IceniColorScheme,
        typography  = IceniTypography,
        content     = content
    )
}
