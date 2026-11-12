package com.nndiph.liftverse.feature.logger.session

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.domain.formula.LiveAchievement

/** Number badge of a set (32 dp, radius 8); warm-ups show "W" in a dashed outline. */
@Composable
internal fun SetBadge(number: Int?, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = MaterialTheme.shapes.small
    val frame = if (number == null) {
        Modifier.drawBehind {
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            )
            val inset = stroke.width / 2
            drawRoundRect(
                color = outline,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                cornerRadius = CornerRadius(8.dp.toPx() - inset),
                style = stroke
            )
        }
    } else {
        Modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)
    }
    Box(modifier = modifier.size(32.dp).then(frame), contentAlignment = Alignment.Center) {
        Text(
            text = number?.toString() ?: stringResource(R.string.set_warmup_badge),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** The tick of a set: a filled circle when done, a 2 dp ring when not (32 dp inside a 52 dp target). */
@Composable
internal fun SetCheckButton(done: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(if (done) R.string.action_uncomplete_set else R.string.action_complete_set_check)
    Box(
        modifier = modifier
            .size(52.dp)
            .toggleable(
                value = done,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        DoneCircle(done = done, size = 32.dp)
    }
}

@Composable
internal fun DoneCircle(done: Boolean, size: Dp, modifier: Modifier = Modifier) {
    if (done) {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(LiftverseTheme.colors.primaryFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = LiftverseTheme.colors.onPrimaryFill,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    } else {
        Box(modifier = modifier.size(size).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
    }
}

/** Tag on a done compact row (22 dp, radius 6): volt "↑ +2,5 kg" or brass "PR". */
@Composable
internal fun SetTagChip(tag: SetTag, modifier: Modifier = Modifier) {
    when (tag) {
        SetTag.None -> Unit
        is SetTag.Improved -> Tag(
            container = LiftverseTheme.colors.accent,
            content = LiftverseTheme.colors.onAccent,
            modifier = modifier
        ) {
            Text(stringResource(R.string.achievement_up, improvementText(tag.improvement)), style = tagTextStyle())
        }
        SetTag.Pr -> Tag(container = LiftverseTheme.colors.brass, content = LiftverseTheme.colors.onBrass, modifier = modifier) {
            Icon(painterResource(R.drawable.ic_trophy_filled), contentDescription = null, modifier = Modifier.size(12.dp))
            Text(stringResource(R.string.tag_pr), style = tagTextStyle())
        }
    }
}

/** "Custom" tag beside the name of a user-made exercise. */
@Composable
internal fun CustomTag(modifier: Modifier = Modifier) {
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
            style = tagTextStyle(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Tag(container: Color, content: Color, modifier: Modifier = Modifier, body: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            modifier = modifier
                .height(22.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(container)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = body
        )
    }
}

@Composable
private fun tagTextStyle() = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)

/**
 * Live chip of the active set (28 dp, radius 8). TalkBack hears the detail through a polite live region, and a
 * short vibration marks the moment it appears (README "Pencapaian langsung").
 */
@Composable
internal fun AchievementChip(achievement: LiveAchievement, modifier: Modifier = Modifier) {
    if (achievement == LiveAchievement.None) return
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(achievement::class) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    val description = when (achievement) {
        is LiveAchievement.Improved -> improvementDescription(achievement.improvement)
        is LiveAchievement.PrCandidate -> stringResource(
            R.string.achievement_pr_description,
            formatDecimal(achievement.estimatedKg, maxFractionDigits = 1),
            formatDecimal(achievement.recordKg, maxFractionDigits = 1)
        )
        LiveAchievement.None -> ""
    }
    val isRecord = achievement is LiveAchievement.PrCandidate
    val container = if (isRecord) LiftverseTheme.colors.brass else LiftverseTheme.colors.accent
    val content = if (isRecord) LiftverseTheme.colors.onBrass else LiftverseTheme.colors.onAccent
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            modifier = modifier
                .height(28.dp)
                .clip(MaterialTheme.shapes.small)
                .background(container)
                .padding(horizontal = 10.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            if (achievement is LiveAchievement.Improved) {
                Text(stringResource(R.string.achievement_up, improvementText(achievement.improvement)), style = textStyle)
            } else {
                Icon(painterResource(R.drawable.ic_trophy_filled), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.achievement_pr), style = textStyle)
            }
        }
    }
}

/**
 * Sync indicator (28 dp chip). Sync to the cloud is not built yet, so the honest state is
 * "Tersimpan di perangkat"; "Tersinkron" and "Menyinkronkan" come with the sync worker.
 */
@Composable
internal fun SyncChip(modifier: Modifier = Modifier) {
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
internal fun PrimaryFillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null
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
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
