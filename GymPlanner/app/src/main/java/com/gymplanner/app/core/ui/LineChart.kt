package com.gymplanner.app.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class ChartPoint(val x: Long, val y: Double)

/**
 * Simple line chart drawn on a Canvas (no chart library).
 * [points] are drawn as dots; [line] (e.g. a 7-day average) as a smooth line.
 * If [line] is null, the points themselves are joined.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    line: List<ChartPoint>? = null,
    color: Color = AppTheme.colors.accent,
    yLabel: (Double) -> String = { com.gymplanner.app.core.model.Units.formatNumber(it) },
    xLabel: (Long) -> String = { it.toString() },
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = AppTheme.type.caption.copy(color = AppTheme.colors.secondaryLabel)
    val gridColor = AppTheme.colors.separator
    val dotColor = if (line != null) color.copy(alpha = 0.45f) else color

    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        if (points.isEmpty()) return@Canvas
        val all = points + (line ?: emptyList())
        var minY = all.minOf { it.y }
        var maxY = all.maxOf { it.y }
        if (abs(maxY - minY) < 1.0) { minY -= 1.0; maxY += 1.0 }
        val pad = (maxY - minY) * 0.12
        minY = floor(minY - pad); maxY = ceil(maxY + pad)
        val minX = all.minOf { it.x }
        val maxX = max(all.maxOf { it.x }, minX + 1)

        val labelWidth = 44.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val chartW = size.width - labelWidth
        val chartH = size.height - bottomPad
        fun px(x: Long) = ((x - minX).toFloat() / (maxX - minX)) * chartW
        fun py(y: Double) = (chartH - ((y - minY) / (maxY - minY)) * chartH).toFloat()

        // Grid lines + y labels on the right (like Apple Health)
        val lines = 4
        for (i in 0..lines) {
            val v = minY + (maxY - minY) * i / lines
            val y = py(v)
            drawLine(
                gridColor, Offset(0f, y), Offset(chartW, y), strokeWidth = 1f,
                pathEffect = if (i == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            val layout = measurer.measure(yLabel(v), labelStyle)
            drawText(layout, topLeft = Offset(chartW + 6.dp.toPx(), y - layout.size.height / 2f))
        }
        // x labels: first and last
        val first = measurer.measure(xLabel(minX), labelStyle)
        drawText(first, topLeft = Offset(0f, chartH + 4.dp.toPx()))
        if (maxX != minX) {
            val last = measurer.measure(xLabel(all.maxOf { it.x }), labelStyle)
            drawText(last, topLeft = Offset(chartW - last.size.width, chartH + 4.dp.toPx()))
        }

        val series = (line ?: points).sortedBy { it.x }
        if (series.size > 1) {
            val path = Path()
            series.forEachIndexed { i, p ->
                val x = px(p.x); val y = py(p.y)
                if (i == 0) path.moveTo(x, y) else {
                    val prev = series[i - 1]
                    val cx = (px(prev.x) + x) / 2f
                    path.cubicTo(cx, py(prev.y), cx, y, x, y)
                }
            }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val r = if (points.size > 60) 2.5.dp.toPx() else 4.dp.toPx()
        points.forEach { p -> drawCircle(dotColor, radius = r, center = Offset(px(p.x), py(p.y))) }
    }
}
