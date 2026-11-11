package com.nndiph.liftverse.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// PROVISIONAL palette: "blued steel" cobalt, cool slate neutrals, and one brass accent.
// Follows the owner's direction (clean, modern, blue primary, light + dark). Hex values are
// proposals until the Claude Design handoff exists; replace them from that handoff.
//
// Role mapping rules:
// - primary / secondary are both blue, so default M3 components (NavigationBar indicator,
//   chips, FAB) stay on-brand.
// - brass lives in `tertiary` and is used only for PRs and streaks.
// - every surface / surfaceContainer role is set explicitly; unset roles fall back to the
//   M3 baseline purple.

// Cobalt (primary)
private val Cobalt10 = Color(0xFF001550)
private val Cobalt20 = Color(0xFF00267F)
private val Cobalt30 = Color(0xFF1A3DB8)
private val Cobalt40 = Color(0xFF2052E9)
private val Cobalt80 = Color(0xFF7098FF)
private val Cobalt90 = Color(0xFFDCE4FF)

// Steel blue (secondary): desaturated cobalt
private val Steel10 = Color(0xFF111A2E)
private val Steel20 = Color(0xFF263045)
private val Steel30 = Color(0xFF3C465C)
private val Steel40 = Color(0xFF535E75)
private val Steel80 = Color(0xFFBBC6E0)
private val Steel90 = Color(0xFFD9E2FC)

// Brass (tertiary): PRs and streaks only
private val Brass10 = Color(0xFF261A00)
private val Brass20 = Color(0xFF3F2E00)
private val Brass30 = Color(0xFF5B4300)
private val Brass40 = Color(0xFF795A00)
private val Brass80 = Color(0xFFEDC15A)
private val Brass90 = Color(0xFFFFDF9C)

// Slate neutrals
private val Slate4 = Color(0xFF070B16)
private val Slate5 = Color(0xFF0A0F1C)
private val Slate10 = Color(0xFF151B2B)
private val Slate12 = Color(0xFF191F2F)
private val Slate17 = Color(0xFF222839)
private val Slate20 = Color(0xFF2A3142)
private val Slate22 = Color(0xFF2D3446)
private val Slate24 = Color(0xFF31384A)
private val Slate30 = Color(0xFF414859)
private val Slate50 = Color(0xFF717889)
private val Slate60 = Color(0xFF8B92A3)
private val Slate80 = Color(0xFFC1C6D6)
private val Slate87 = Color(0xFFD6DBE8)
private val Slate90 = Color(0xFFDDE2F1)
private val Slate92 = Color(0xFFE4E8F3)
private val Slate94 = Color(0xFFEAEDF5)
private val Slate95 = Color(0xFFEEF1F7)
private val Slate96 = Color(0xFFF2F4F9)
private val Slate97 = Color(0xFFF6F8FB)

private val Error10 = Color(0xFF410002)
private val Error20 = Color(0xFF690005)
private val Error30 = Color(0xFF93000A)
private val Error40 = Color(0xFFBA1A1A)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)

internal val LightColorScheme =
    lightColorScheme(
        primary = Cobalt40,
        onPrimary = Color.White,
        primaryContainer = Cobalt90,
        onPrimaryContainer = Cobalt10,
        inversePrimary = Cobalt80,
        secondary = Steel40,
        onSecondary = Color.White,
        secondaryContainer = Steel90,
        onSecondaryContainer = Steel10,
        tertiary = Brass40,
        onTertiary = Color.White,
        tertiaryContainer = Brass90,
        onTertiaryContainer = Brass10,
        background = Slate97,
        onBackground = Slate10,
        surface = Slate97,
        onSurface = Slate10,
        surfaceVariant = Slate90,
        onSurfaceVariant = Slate30,
        surfaceTint = Cobalt40,
        inverseSurface = Slate20,
        inverseOnSurface = Slate95,
        error = Error40,
        onError = Color.White,
        errorContainer = Error90,
        onErrorContainer = Error10,
        outline = Slate50,
        outlineVariant = Slate80,
        scrim = Color.Black,
        surfaceBright = Slate97,
        surfaceDim = Slate87,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Slate96,
        surfaceContainer = Slate94,
        surfaceContainerHigh = Slate92,
        surfaceContainerHighest = Slate90
    )

internal val DarkColorScheme =
    darkColorScheme(
        primary = Cobalt80,
        onPrimary = Cobalt20,
        primaryContainer = Cobalt30,
        onPrimaryContainer = Cobalt90,
        inversePrimary = Cobalt40,
        secondary = Steel80,
        onSecondary = Steel20,
        secondaryContainer = Steel30,
        onSecondaryContainer = Steel90,
        tertiary = Brass80,
        onTertiary = Brass20,
        tertiaryContainer = Brass30,
        onTertiaryContainer = Brass90,
        background = Slate5,
        onBackground = Slate90,
        surface = Slate5,
        onSurface = Slate90,
        surfaceVariant = Slate30,
        onSurfaceVariant = Slate80,
        surfaceTint = Cobalt80,
        inverseSurface = Slate90,
        inverseOnSurface = Slate20,
        error = Error80,
        onError = Error20,
        errorContainer = Error30,
        onErrorContainer = Error90,
        outline = Slate60,
        outlineVariant = Slate30,
        scrim = Color.Black,
        surfaceBright = Slate24,
        surfaceDim = Slate5,
        surfaceContainerLowest = Slate4,
        surfaceContainerLow = Slate10,
        surfaceContainer = Slate12,
        surfaceContainerHigh = Slate17,
        surfaceContainerHighest = Slate22
    )
