package com.nndiph.liftverse.feature.logger.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.ThemePreviews
import kotlinx.coroutines.launch

/** What the numpad edits. Kept in saved state, so it survives rotation. */
data class NumpadRequest(val setId: String, val field: SetField)

/**
 * Numpad bottom sheet (README "Baris set": "Ketuk angka → numpad dengan ±langkah, koma, hapus, Simpan"). Opened by
 * tapping a stepper value; "Simpan" hands the typed value to [onSave], then the sheet slides away and
 * [onDismiss] removes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumpadSheet(
    title: String,
    previousLabel: String,
    unit: String,
    initialValue: Double,
    step: Double,
    allowDecimal: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        NumpadContent(
            title = title,
            previousLabel = previousLabel,
            unit = unit,
            initialValue = initialValue,
            step = step,
            allowDecimal = allowDecimal,
            onSave = { value ->
                onSave(value)
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }
}

/** The sheet body: title row, the typed value, and a 4 × 4 key grid ending in a two-column "Simpan". */
@Composable
fun NumpadContent(
    title: String,
    previousLabel: String,
    unit: String,
    initialValue: Double,
    step: Double,
    allowDecimal: Boolean,
    onSave: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var buffer by rememberSaveable(initialValue, stateSaver = NumpadBuffer.Saver) {
        mutableStateOf(NumpadBuffer.of(initialValue, allowDecimal))
    }
    val stepText = formatDecimal(step)
    Column(modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)
            )
            Text(
                text = previousLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(10.dp))
        NumpadDisplay(text = buffer.displayText, unit = unit)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyRow {
                listOf(1, 2, 3).forEach { DigitKey(it) { buffer = buffer.digit(it) } }
                SideKey(stringResource(R.string.numpad_step_minus, stepText)) { buffer = buffer.step(-step, allowDecimal) }
            }
            KeyRow {
                listOf(4, 5, 6).forEach { DigitKey(it) { buffer = buffer.digit(it) } }
                SideKey(stringResource(R.string.numpad_step_plus, stepText)) { buffer = buffer.step(step, allowDecimal) }
            }
            KeyRow {
                listOf(7, 8, 9).forEach { DigitKey(it) { buffer = buffer.digit(it) } }
                DeleteKey { buffer = buffer.delete() }
            }
            KeyRow {
                Key(
                    label = stringResource(R.string.numpad_decimal),
                    container = MaterialTheme.colorScheme.surface,
                    enabled = allowDecimal,
                    modifier = Modifier.weight(1f)
                ) { buffer = buffer.comma() }
                DigitKey(0) { buffer = buffer.digit(0) }
                Key(
                    label = stringResource(R.string.numpad_save),
                    container = LiftverseTheme.colors.primaryFill,
                    content = LiftverseTheme.colors.onPrimaryFill,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(2f)
                ) { onSave(buffer.value()) }
            }
        }
    }
}

@Composable
private fun NumpadDisplay(text: String, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        // Caret: the value is being typed.
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .width(2.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(text = unit, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun RowScope.DigitKey(digit: Int, onClick: () -> Unit) {
    Key(
        label = digit.toString(),
        container = MaterialTheme.colorScheme.surface,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.weight(1f),
        onClick = onClick
    )
}

@Composable
private fun RowScope.SideKey(label: String, onClick: () -> Unit) {
    Key(
        label = label,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.weight(1f),
        onClick = onClick
    )
}

@Composable
private fun RowScope.DeleteKey(onClick: () -> Unit) {
    val description = stringResource(R.string.numpad_delete)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(KEY_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(R.drawable.ic_backspace), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Key(
    label: String,
    container: Color,
    modifier: Modifier = Modifier,
    content: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = style, color = content)
    }
}

private val KEY_HEIGHT = 56.dp
private const val DISABLED_ALPHA = 0.38f

@ThemePreviews
@Composable
private fun NumpadSheetPreview() {
    LiftversePreview {
        Column(
            Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(top = 16.dp)
        ) {
            NumpadContent(
                title = "Beban · Barbell Bench Press",
                previousLabel = "Sesi lalu 80 kg × 8",
                unit = "kg",
                initialValue = 82.5,
                step = 2.5,
                allowDecimal = true,
                onSave = {}
            )
        }
    }
}
