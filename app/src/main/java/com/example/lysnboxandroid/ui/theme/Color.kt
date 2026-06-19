package com.example.lysnboxandroid.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// MARK: - Adaptive semantic tokens (ported from iOS Theme.swift)

// Light
val J7AppBackground = Color(0xFFF7F8FC)
val J7Surface = Color(0xFFFFFFFF)
val J7Border = Color(0xFFE6E8EC)
val J7Accent = Color(0xFF3B5284)
val J7TextPrimary = Color(0xFF0F172A)
val J7TextSecondary = Color(0xFF475569)

// Dark
val J7AppBackgroundDark = Color(0xFF0C0C0E)
val J7SurfaceDark = Color(0xFF18181B)
val J7BorderDark = Color(0xFF2C2C30)
val J7AccentDark = Color(0xFFA5B4FC)
val J7TextPrimaryDark = Color(0xFFFFFFFF)
val J7TextSecondaryDark = Color(0xFFA1A1A6)

/**
 * A reading theme palette, mirroring iOS `ThemePalette` in Sources/App/Theme.swift.
 * Applied to the reader canvas independently of system light/dark mode.
 */
data class ThemePalette(
    val id: String,
    val displayName: String,
    val accent: Color,
    val appBackground: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val activeParagraphBg: Color,
    val activeSentenceBg: Color,
    val activeWordBg: Color,
    val activeWordFg: Color,
) {
    companion object {
        val ModernBlue = ThemePalette(
            id = "modernBlue",
            displayName = "Modern Blue",
            accent = Color(0xFF3B5284),
            appBackground = Color(0xFFF8F9FA),
            surface = Color(0xFFFFFFFF),
            border = Color(0xFFE6E8EC),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            activeParagraphBg = Color(0xFFF0F4FA),
            activeSentenceBg = Color(0xFFDCE6F5),
            activeWordBg = Color(0xFFFFE066),
            activeWordFg = Color(0xFF0F172A),
        )

        val WarmIvory = ThemePalette(
            id = "warmIvory",
            displayName = "Warm Ivory",
            accent = Color(0xFFC59B27),
            appBackground = Color(0xFFFDFBF7),
            surface = Color(0xFFFCFAF4),
            border = Color(0xFFF2EDE4),
            textPrimary = Color(0xFF1C160C),
            textSecondary = Color(0xFF5C564C),
            activeParagraphBg = Color(0xFFF5EFEB),
            activeSentenceBg = Color(0xFFFDF1DB),
            activeWordBg = Color(0xFFEAD2AC),
            activeWordFg = Color(0xFF1C160C),
        )

        val SoothingGreen = ThemePalette(
            id = "soothingGreen",
            displayName = "Soothing Green",
            accent = Color(0xFF2E7D32),
            appBackground = Color(0xFFF6F8F6),
            surface = Color(0xFFF2F5F2),
            border = Color(0xFFE2EAE2),
            textPrimary = Color(0xFF112A18),
            textSecondary = Color(0xFF455E4C),
            activeParagraphBg = Color(0xFFE8EFE9),
            activeSentenceBg = Color(0xFFDCEADF),
            activeWordBg = Color(0xFFB7DEC2),
            activeWordFg = Color(0xFF112A18),
        )

        val MidnightDark = ThemePalette(
            id = "midnightDark",
            displayName = "Midnight Dark",
            accent = Color(0xFFA5B4FC), // Indigo 300
            appBackground = Color(0xFF0C0C0E),
            surface = Color(0xFF18181B),
            border = Color(0xFF2C2C30),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1A6),
            activeParagraphBg = Color(0xFF242427),
            activeSentenceBg = Color(0xFF2C2D35),
            activeWordBg = Color(0xFF4338CA), // Deep Indigo
            activeWordFg = Color(0xFFFFFFFF),
        )

        val all = listOf(ModernBlue, WarmIvory, SoothingGreen, MidnightDark)

        fun byId(id: String?): ThemePalette = all.firstOrNull { it.id == id } ?: ModernBlue
    }
}

/** Active reading palette, provided down the tree so the reader can swap palettes live. */
val LocalPalette = staticCompositionLocalOf { ThemePalette.ModernBlue }
