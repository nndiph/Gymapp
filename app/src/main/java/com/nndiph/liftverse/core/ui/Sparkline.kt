package com.nndiph.liftverse.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Sparkline (SPEC components: 56×24 on the dashboard, 64×28 in lists): a 2 dp [color] line through [values]
 * (oldest first) with a 3 dp dot on the last one. The lowest value sits at the bottom, the highest at the top.
 */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val inset = 3.dp.toPx()
        val low = values.min()
        val high = values.max()
        val range = (high - low).takeIf { it > 0.0 } ?: 1.0
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) {
                size.width - inset
            } else {
                inset + index * (size.width - 2 * inset) / (values.size - 1)
            }
            Offset(x, inset + ((high - value) / range).toFloat() * (size.height - 2 * inset))
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(color, radius = 3.dp.toPx(), center = points.last())
    }
}
