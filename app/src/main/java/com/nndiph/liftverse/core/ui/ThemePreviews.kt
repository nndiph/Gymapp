package com.nndiph.liftverse.core.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nndiph.liftverse.core.theme.LiftverseTheme

/** Renders a preview twice: light and dark. Compare with design_handoff_liftverse/screenshots/{light,dark}. */
@Preview(name = "Light", group = "Theme")
@Preview(name = "Dark", group = "Theme", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class ThemePreviews

/** Whole screens at the design artboard size (412 × 915 dp), light and dark. */
@Preview(name = "Light", group = "Screen", widthDp = 412, heightDp = 915)
@Preview(
    name = "Dark",
    group = "Screen",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class ScreenPreviews

/** The app theme on the screen background, for previews. */
@Composable
fun LiftversePreview(content: @Composable () -> Unit) {
    LiftverseTheme {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
