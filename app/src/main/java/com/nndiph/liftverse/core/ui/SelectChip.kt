package com.nndiph.liftverse.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.R

/** 32 dp chip, radius 8 (SPEC "Chip filter"); the touch target is 48 dp. */
@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.small
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(shape)
                .background(if (selected) colors.primaryContainer else colors.background)
                .border(1.dp, if (selected) colors.primaryContainer else colors.outline, shape)
                .padding(start = if (selected) 8.dp else 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                maxLines = 1
            )
        }
    }
}
