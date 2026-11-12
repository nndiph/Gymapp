package app.liftverse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.liftverse.R

// Fonts (Google Fonts, OFL):
//   Archivo SemiExpanded (width 112.5) static files: archivo_semiexpanded_bold.ttf, _extrabold.ttf
//   Plus Jakarta Sans: plus_jakarta_sans_regular/medium/semibold/bold.ttf
// Archivo = numbers + large titles only. Jakarta = all UI text. Never all caps.
// All numbers use tabular figures ("tnum").

val Archivo = FontFamily(
    Font(R.font.archivo_semiexpanded_bold, FontWeight.Bold),
    Font(R.font.archivo_semiexpanded_extrabold, FontWeight.ExtraBold),
)

val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

private const val TNUM = "tnum"

private fun archivo(size: Int, line: Int, weight: FontWeight = FontWeight.ExtraBold, tracking: Double = 0.0) = TextStyle(
    fontFamily = Archivo, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = tracking.em, fontFeatureSettings = TNUM,
)

private fun jakarta(size: Int, line: Int, weight: FontWeight) = TextStyle(
    fontFamily = Jakarta, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = 0.sp, fontFeatureSettings = TNUM,
)

val LiftverseTypography = Typography(
    displayLarge = archivo(56, 60, tracking = -0.01),   // numDisplay: 41.200
    displayMedium = archivo(36, 40, FontWeight.Bold),     // numHeadline: 116,7 kg
    displaySmall = archivo(32, 38),                       // "Sesi selesai", dashboard stats
    headlineLarge = archivo(30, 34),                      // "Push Day" on today card
    headlineMedium = archivo(28, 34),                     // onboarding titles
    headlineSmall = archivo(24, 30, tracking = -0.01),    // wordmark in top bar
    titleLarge = jakarta(22, 28, FontWeight.Bold),        // top app bar, display name
    titleMedium = jakarta(16, 24, FontWeight.SemiBold),   // exercise name, list title
    titleSmall = jakarta(14, 20, FontWeight.Bold),        // 48dp button label, section title
    bodyLarge = jakarta(16, 24, FontWeight.Normal),
    bodyMedium = jakarta(14, 20, FontWeight.Normal),
    bodySmall = jakarta(12, 16, FontWeight.Normal),
    labelLarge = jakarta(16, 24, FontWeight.Bold),        // 56dp primary button label
    labelMedium = jakarta(12, 16, FontWeight.SemiBold),
    labelSmall = jakarta(11, 16, FontWeight.SemiBold),    // tags (Custom, PR), heatmap axis
)

/** Number styles that do not map to M3 slots. */
@Immutable
data class LiftverseNumberStyles(
    val stepper: TextStyle = archivo(28, 32),               // active set stepper value
    val title: TextStyle = archivo(22, 28, FontWeight.Bold), // 80 kg × 8
    val setRow: TextStyle = archivo(20, 24, FontWeight.Bold),// compact set row values
    val stat: TextStyle = archivo(26, 32),                   // profile month stats
    val small: TextStyle = archivo(16, 20, FontWeight.Bold), // follower counts, timer
)

val LocalLiftverseNumbers = staticCompositionLocalOf { LiftverseNumberStyles() }
