package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SyneFontFamily = FontFamily(
    Font(R.font.syne, FontWeight.Normal),
    Font(R.font.syne, FontWeight.Medium),
    Font(R.font.syne, FontWeight.SemiBold),
    Font(R.font.syne, FontWeight.Bold)
)

val CormorantFontFamily = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.Medium),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond, FontWeight.Bold)
)

val JakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val LithfitWordmarkStyle = TextStyle(
    fontFamily = SyneFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    letterSpacing = 5.sp
)

fun buildLithfitTypography(largeReadableText: Boolean = false): Typography {
    val scale = if (largeReadableText) 1.14f else 1f
    return Typography(
        displayLarge = TextStyle(
            fontFamily = CormorantFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (40f * scale).sp,
            lineHeight = (44f * scale).sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = CormorantFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (32f * scale).sp,
            lineHeight = (36f * scale).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = SyneFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (26f * scale).sp,
            lineHeight = (32f * scale).sp,
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = SyneFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22f * scale).sp,
            lineHeight = (28f * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * scale).sp,
            lineHeight = (24f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * scale).sp,
            lineHeight = (24f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * scale).sp,
            lineHeight = (21f * scale).sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13f * scale).sp,
            lineHeight = (18f * scale).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * scale).sp,
            lineHeight = (17f * scale).sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * scale).sp,
            lineHeight = (18f * scale).sp,
            letterSpacing = 0.3.sp
        ),
        labelMedium = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * scale).sp,
            lineHeight = (16f * scale).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10f * scale).sp,
            lineHeight = (14f * scale).sp,
            letterSpacing = 0.6.sp
        )
    )
}

val LithfitTypography = buildLithfitTypography(largeReadableText = false)
