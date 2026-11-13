package com.nndiph.liftverse.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.theme.LocalLiftverseColors

// SPEC "Warna": text on volt cards is #1C2400 (onAccent) and #3E4C12 in both themes.
private val VoltInkVariant = Color(0xFF3E4C12)
private const val VOLT_CONTAINER_ALPHA = 0.14f

/**
 * A volt card (dashboard streak card, "Sesi selesai" hero). The fill stays volt in both themes, so the content
 * gets fixed dark inks: the prototype overrides the same tokens inside these cards (onSurface, onSurfaceVariant,
 * surfaceContainer, outline, primary, primaryFill, onPrimaryFill, accentText).
 */
@Composable
fun VoltSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    val colors = LiftverseTheme.colors
    val ink = colors.onAccent
    val scheme = MaterialTheme.colorScheme.copy(
        onSurface = ink,
        onSurfaceVariant = VoltInkVariant,
        surfaceContainer = ink.copy(alpha = VOLT_CONTAINER_ALPHA),
        outline = VoltInkVariant,
        primary = ink
    )
    val extended = colors.copy(primaryFill = ink, onPrimaryFill = colors.accent, accentText = ink)
    Box(modifier.clip(shape).background(colors.accent)) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
            CompositionLocalProvider(LocalLiftverseColors provides extended, LocalContentColor provides ink) {
                content()
            }
        }
    }
}

/**
 * Edge around a small volt fill on a surface (prototype `--markEdge`): onAccent on light surfaces, where volt on
 * white is only 1.35:1, and none on dark ones.
 */
@Composable
fun accentEdgeColor(): Color = if (MaterialTheme.colorScheme.surface.luminance() > LIGHT_SURFACE_LUMINANCE) {
    LiftverseTheme.colors.onAccent
} else {
    Color.Transparent
}

private const val LIGHT_SURFACE_LUMINANCE = 0.5f
