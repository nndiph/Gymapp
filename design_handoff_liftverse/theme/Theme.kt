package app.liftverse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Radius hierarchy. Never one radius for everything.
val LiftverseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // tags: Custom, PR, "Hari ini"
    small = RoundedCornerShape(8.dp),        // filter chips, set number badge, routine tile
    medium = RoundedCornerShape(12.dp),      // buttons, text fields, steppers, banners
    large = RoundedCornerShape(16.dp),       // cards, active set panel, FAB
    extraLarge = RoundedCornerShape(28.dp),  // bottom sheets (top corners), dialogs
)
val HeatmapCellShape = RoundedCornerShape(2.dp)

@Immutable
data class LiftverseSpacing(
    val xxs: Dp = 4.dp,    // icon to label
    val xs: Dp = 8.dp,     // between chips / buttons
    val sm: Dp = 12.dp,    // between cards, stepper gap
    val md: Dp = 16.dp,    // screen margin, card padding
    val lg: Dp = 24.dp,    // between sections
    val xl: Dp = 32.dp,    // large sections, empty states
    val touch: Dp = 48.dp, // minimum touch target
)

val LocalLiftverseSpacing = staticCompositionLocalOf { LiftverseSpacing() }

@Composable
fun LiftverseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color intentionally NOT used: the blued-steel identity must stay consistent.
    val scheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(
        LocalLiftverseColors provides extended,
        LocalLiftverseSpacing provides LiftverseSpacing(),
        LocalLiftverseNumbers provides LiftverseNumberStyles(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LiftverseTypography,
            shapes = LiftverseShapes,
            content = content,
        )
    }
}

object LiftverseTheme {
    val colors: LiftverseExtendedColors
        @Composable @ReadOnlyComposable get() = LocalLiftverseColors.current
    val spacing: LiftverseSpacing
        @Composable @ReadOnlyComposable get() = LocalLiftverseSpacing.current
    val numbers: LiftverseNumberStyles
        @Composable @ReadOnlyComposable get() = LocalLiftverseNumbers.current
}
