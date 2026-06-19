package com.example.lysnboxandroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Serif for reading content & book titles (New York / Charter feel on iOS).
val SerifFamily = FontFamily.Serif
val SansFamily = FontFamily.SansSerif

/**
 * Named text styles ported from iOS `Font+Theme.swift` (the j7* scale).
 * UI uses sans; reading content and book titles use serif.
 */
object LysnType {
    val titleLarge = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 34.sp)
    val title1Serif = TextStyle(fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
    val title1 = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
    val title2 = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val title3 = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
    val title3Serif = TextStyle(fontFamily = SerifFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)

    val body = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodyMedium = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySerif = TextStyle(fontFamily = SerifFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp)

    val subheadline = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val subheadlineSemibold = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
    val subheadlineSerifBold = TextStyle(fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp)

    val caption = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp)
    val captionMedium = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp)
    val caption2 = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 12.sp)
}

val Typography = Typography(
    titleLarge = LysnType.titleLarge,
    titleMedium = LysnType.title2,
    titleSmall = LysnType.title3,
    bodyLarge = LysnType.bodySerif,
    bodyMedium = LysnType.body,
    bodySmall = LysnType.subheadline,
    labelLarge = LysnType.bodyMedium,
    labelMedium = LysnType.subheadlineSemibold,
    labelSmall = LysnType.caption,
)
