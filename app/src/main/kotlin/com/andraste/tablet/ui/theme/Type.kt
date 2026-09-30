package com.andraste.tablet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val IceniTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.Bold,
        fontSize     = 22.sp,
        color        = IceniGold,
        letterSpacing = 2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        color        = IceniGold
    ),
    titleLarge = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        color        = IceniText
    ),
    titleMedium = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        color        = IceniText
    ),
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontSize     = 14.sp,
        color        = IceniText
    ),
    bodyMedium = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontSize     = 12.sp,
        color        = IceniTextMuted
    ),
    bodySmall = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontSize     = 11.sp,
        color        = IceniTextHint
    ),
    labelLarge = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.Medium,
        fontSize     = 13.sp,
        color        = IceniGold,
        letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontSize     = 10.sp,
        color        = IceniTextHint,
        letterSpacing = 1.sp
    )
)
