package com.gymplanner.app.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymplanner.app.core.model.Muscles

/**
 * Front and back outline of a body. Primary muscles are filled with the accent,
 * secondary ones with the accent at 50 %, everything else stays grey.
 * Drawn on a 100 × 210 grid per figure.
 */
@Composable
fun BodyMap(primary: Set<String>, secondary: Set<String>, height: Dp = 190.dp) {
    val accent = AppTheme.colors.accent
    val base = AppTheme.colors.fill
    val idle = AppTheme.colors.outline
    fun colorFor(m: String): Color = when (m) {
        in primary -> accent
        in secondary -> accent.copy(alpha = 0.5f)
        else -> idle
    }
    val description = buildString {
        append("Body map. ")
        if (primary.isNotEmpty()) append("Main: " + primary.joinToString { Muscles.label(it) } + ". ")
        if (secondary.isNotEmpty()) append("Also: " + secondary.joinToString { Muscles.label(it) } + ".")
    }
    Row(
        Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(true, false).forEach { front ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(width = height * (100f / 210f), height = height)) {
                    val s = size.height / 210f
                    drawFigure(s, base, front, ::colorFor)
                }
                Text(if (front) "Front" else "Back", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
            }
        }
    }
}

private fun DrawScope.rr(s: Float, x1: Float, y1: Float, x2: Float, y2: Float, c: Color, r: Float = 5f) =
    drawRoundRect(c, Offset(x1 * s, y1 * s), Size((x2 - x1) * s, (y2 - y1) * s), CornerRadius(r * s, r * s))

/** Draws the shape and its mirror on the other side of the body. */
private fun DrawScope.pair(s: Float, x1: Float, y1: Float, x2: Float, y2: Float, c: Color, r: Float = 5f) {
    rr(s, x1, y1, x2, y2, c, r)
    rr(s, 100 - x2, y1, 100 - x1, y2, c, r)
}

private fun DrawScope.poly(s: Float, c: Color, vararg pts: Pair<Float, Float>) {
    val p = Path()
    pts.forEachIndexed { i, (x, y) -> if (i == 0) p.moveTo(x * s, y * s) else p.lineTo(x * s, y * s) }
    p.close()
    drawPath(p, c)
    val m = Path()
    pts.forEachIndexed { i, (x, y) -> if (i == 0) m.moveTo((100 - x) * s, y * s) else m.lineTo((100 - x) * s, y * s) }
    m.close()
    drawPath(m, c)
}

private fun DrawScope.drawFigure(s: Float, base: Color, front: Boolean, color: (String) -> Color) {
    // Silhouette
    drawCircle(base, 10f * s, Offset(50f * s, 14f * s))
    rr(s, 45f, 22f, 55f, 32f, base, 2f)
    rr(s, 31f, 30f, 69f, 98f, base, 12f)          // torso
    pair(s, 18f, 34f, 30f, 76f, base, 7f)         // upper arms
    pair(s, 13f, 72f, 26f, 106f, base, 6f)        // forearms
    drawCircle(base, 4.5f * s, Offset(19f * s, 110f * s)); drawCircle(base, 4.5f * s, Offset(81f * s, 110f * s))
    pair(s, 32f, 94f, 49.5f, 150f, base, 9f)      // thighs
    pair(s, 34f, 148f, 48f, 196f, base, 7f)       // lower legs
    pair(s, 33f, 194f, 48f, 202f, base, 3f)       // feet

    if (front) {
        // Shoulders: side delts on the outside, front delts toward the chest
        pair(s, 21f, 32f, 31f, 46f, color("side_delts"), 6f)
        pair(s, 27f, 33f, 35f, 47f, color("front_delts"), 5f)
        pair(s, 35f, 34f, 49.2f, 54f, color("chest"), 6f)
        pair(s, 19f, 48f, 28f, 72f, color("biceps"), 5f)
        pair(s, 14f, 75f, 25f, 103f, color("forearms"), 5f)
        rr(s, 41f, 57f, 59f, 91f, color("abs"), 4f)
        pair(s, 33f, 58f, 39.5f, 90f, color("obliques"), 3f)
        pair(s, 39f, 92f, 48f, 100f, color("hip_flexors"), 3f)
        pair(s, 33f, 101f, 45f, 146f, color("quads"), 7f)
        pair(s, 45.5f, 102f, 49.2f, 132f, color("adductors"), 2f)
        pair(s, 35.5f, 152f, 41f, 186f, color("calves"), 4f)   // the bit of calf visible from the front
    } else {
        poly(s, color("traps"), 50f to 24f, 50f to 46f, 38f to 36f, 34f to 32f, 44f to 27f)
        pair(s, 21f, 32f, 31f, 46f, color("side_delts"), 6f)
        pair(s, 27f, 33f, 35f, 46f, color("rear_delts"), 5f)
        pair(s, 38f, 40f, 49.2f, 56f, color("upper_back"), 4f)
        poly(s, color("lats"), 34f to 46f, 41f to 52f, 47f to 76f, 37f to 72f, 33f to 58f)
        rr(s, 42f, 70f, 58f, 90f, color("lower_back"), 4f)
        pair(s, 19f, 48f, 28f, 72f, color("triceps"), 5f)
        pair(s, 14f, 75f, 25f, 103f, color("forearms"), 5f)
        pair(s, 30.5f, 90f, 34.5f, 108f, color("abductors"), 2f)
        pair(s, 34f, 92f, 49.2f, 114f, color("glutes"), 8f)
        pair(s, 34f, 116f, 48.5f, 148f, color("hamstrings"), 7f)
        pair(s, 35f, 152f, 47f, 186f, color("calves"), 6f)
    }
}
