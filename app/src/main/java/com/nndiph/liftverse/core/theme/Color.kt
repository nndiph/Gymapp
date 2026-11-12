package com.nndiph.liftverse.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Liftverse "grafit + volt". Neutrals are graphite (near-zero chroma). Dynamic color OFF: always use these schemes.
// Rule: primary = olive text/icons (light) or volt (dark); primaryFill = graphite button with volt label (light) or volt button (dark).
// Volt (accent) for identity and effort: wordmark mark, streak, live session. Brass only for PR.

val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF536B00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECF5C4),
    onPrimaryContainer = Color(0xFF2E3B05),
    inversePrimary = Color(0xFFCDEB3B),
    secondary = Color(0xFF4D5754),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E7E5),
    onSecondaryContainer = Color(0xFF1A211E),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBEBCB),
    onTertiaryContainer = Color(0xFF5A3A00),
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF111614),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111614),
    surfaceVariant = Color(0xFFDDE3EC),
    onSurfaceVariant = Color(0xFF49514E),
    surfaceTint = Color.Transparent, // flat: depth via surface steps + 1dp lines, no tonal tint
    inverseSurface = Color(0xFF1E2422),
    inverseOnSurface = Color(0xFFEDF1EF),
    error = Color(0xFFA63D32),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF8E4E1),
    onErrorContainer = Color(0xFF6E1F17),
    outline = Color(0xFF78807D),
    outlineVariant = Color(0xFFD5DBD8),
    scrim = Color(0x73050810),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDE3EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F2F1),
    surfaceContainer = Color(0xFFEAEDEC),
    surfaceContainerHigh = Color(0xFFE2E6E4),
    surfaceContainerHighest = Color(0xFFD9DEDC)
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFCDEB3B), // text, icons, active state
    onPrimary = Color(0xFF1C2400),
    primaryContainer = Color(0xFF2A3510),
    onPrimaryContainer = Color(0xFFE3F59A),
    inversePrimary = Color(0xFF536B00),
    secondary = Color(0xFFB3BCB8),
    onSecondary = Color(0xFF1F2624),
    secondaryContainer = Color(0xFF283030),
    onSecondaryContainer = Color(0xFFD7DEDB),
    tertiary = Color(0xFFE9A93C),
    onTertiary = Color(0xFF2B1C00),
    tertiaryContainer = Color(0xFF3A2A10),
    onTertiaryContainer = Color(0xFFF5CB80),
    background = Color(0xFF0B0D0D),
    onBackground = Color(0xFFEDF1EF),
    surface = Color(0xFF131716),
    onSurface = Color(0xFFEDF1EF),
    surfaceVariant = Color(0xFF29344A),
    onSurfaceVariant = Color(0xFFA7B0AC),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFEDF1EF),
    inverseOnSurface = Color(0xFF1E2422),
    error = Color(0xFFEFA59A),
    onError = Color(0xFF3B0E09),
    errorContainer = Color(0xFF47201B),
    onErrorContainer = Color(0xFFF8D6D0),
    outline = Color(0xFF6F7875),
    outlineVariant = Color(0xFF2A3130),
    scrim = Color(0x99000000),
    surfaceBright = Color(0xFF29344A),
    surfaceDim = Color(0xFF0A0F1C),
    surfaceContainerLowest = Color(0xFF0E1110),
    surfaceContainerLow = Color(0xFF171B1A),
    surfaceContainer = Color(0xFF1B201F),
    surfaceContainerHigh = Color(0xFF222827),
    surfaceContainerHighest = Color(0xFF2A302F)
)

/** Colors outside the Material 3 ColorScheme. Read via LiftverseTheme.colors. */
@Immutable
data class LiftverseExtendedColors(
    /** Filled button / completed-set check background. Light: graphite with volt label. Dark: volt with ink label. */
    val primaryFill: Color,
    val onPrimaryFill: Color,
    /** Volt identity accent: wordmark mark, streak, live session dot/timer, Catat badge.
     *  Light mode: use only as a fill with onAccent text (volt on white is 1.35:1). */
    val accent: Color,
    val onAccent: Color,
    /** Accent as text/icon on surfaces: streak number, session timer. */
    val accentText: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** PR badge fill ONLY. */
    val brass: Color,
    val onBrass: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    /** 5 heatmap levels: 0 = empty … 4 = most active. */
    val heatmap: List<Color>
)

val LightExtendedColors = LiftverseExtendedColors(
    primaryFill = Color(0xFF1A1F14),
    onPrimaryFill = Color(0xFFCDEB3B),
    accent = Color(0xFFCDEB3B),
    onAccent = Color(0xFF1C2400),
    accentText = Color(0xFF536B00),
    accentContainer = Color(0xFFF0F8CF),
    onAccentContainer = Color(0xFF34430A),
    brass = Color(0xFFE9A93C),
    onBrass = Color(0xFF2B1C00),
    success = Color(0xFF2E7652),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE0F0E7),
    onSuccessContainer = Color(0xFF134330),
    heatmap = listOf(
        Color(0xFFE6EAE7),
        Color(0xFFCCE277),
        Color(0xFFA2C22E),
        Color(0xFF6E8C10),
        Color(0xFF3F5206)
    )
)

val DarkExtendedColors = LiftverseExtendedColors(
    primaryFill = Color(0xFFCDEB3B),
    onPrimaryFill = Color(0xFF1C2400),
    accent = Color(0xFFCDEB3B),
    onAccent = Color(0xFF1C2400),
    accentText = Color(0xFFCDEB3B),
    accentContainer = Color(0xFF263010),
    onAccentContainer = Color(0xFFE3F59A),
    brass = Color(0xFFE9A93C),
    onBrass = Color(0xFF2B1C00),
    success = Color(0xFF7DC6A0),
    onSuccess = Color(0xFF0B2A1C),
    successContainer = Color(0xFF15352A),
    onSuccessContainer = Color(0xFFCBEBD9),
    heatmap = listOf(
        Color(0xFF1A1F1E),
        Color(0xFF344516),
        Color(0xFF557220),
        Color(0xFF93B92C),
        Color(0xFFD4F04A)
    )
)

val LocalLiftverseColors = staticCompositionLocalOf { LightExtendedColors }

/** Heatmap level from the day's total volume in kg (0 = no session). */
fun heatmapLevel(volumeKg: Double?): Int = when {
    volumeKg == null -> 0
    volumeKg < 3000 -> 1
    volumeKg < 4500 -> 2
    volumeKg < 6000 -> 3
    else -> 4
}
