package com.nndiph.liftverse.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.theme.LiftverseTheme

// Small pieces shared by several screens.

/** "Custom" tag beside the name of a user-made exercise (22 dp, radius 6, outline). */
@Composable
fun CustomTag(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(22.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = stringResource(R.string.tag_custom),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Sync indicator (28 dp chip). Sync to the cloud is not built yet, so the honest state is
 * "Tersimpan di perangkat"; "Tersinkron" and "Menyinkronkan" come with the sync worker.
 */
@Composable
fun SyncChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_smartphone),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.sync_saved_on_device),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Main action button: primaryFill with the onPrimaryFill label (graphite + volt light, volt dark). */
@Composable
fun PrimaryFillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    iconSize: Dp = 20.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = LiftverseTheme.colors.primaryFill,
            contentColor = LiftverseTheme.colors.onPrimaryFill
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Direction of a change for [DeltaLabel]. */
enum class Trend { UP, DOWN, SAME }

/** "↑ +23%" in success when up; down and unchanged in onSurfaceVariant (SPEC component "Delta"). */
@Composable
fun DeltaLabel(text: String, trend: Trend, style: TextStyle, modifier: Modifier = Modifier, iconSize: Dp = 14.dp) {
    val color = if (trend == Trend.UP) LiftverseTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(
                when (trend) {
                    Trend.UP -> R.drawable.ic_arrow_upward
                    Trend.DOWN -> R.drawable.ic_arrow_downward
                    Trend.SAME -> R.drawable.ic_remove
                }
            ),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(2.dp))
        Text(text = text, style = style, color = color, maxLines = 1)
    }
}
