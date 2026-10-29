package dev.pipos.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val PipBlack = Color(0xFF050A06)
val PipPanel = Color(0xFF0A170E)
val PipGreen = Color(0xFF52F58C)

@Composable
fun PipText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    prominent: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(text, modifier, color = color, fontFamily = FontFamily.Monospace,
        fontWeight = if (prominent) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (prominent) 14.sp else 12.sp,
        lineHeight = 19.sp,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Draw a technical panel with strong corner markers instead of rounded material cards. */
fun Modifier.pipFrame(accent: Color, thickness: Dp = 1.dp): Modifier = this
    .background(PipPanel)
    .border(thickness, accent.copy(alpha = .38f), RectangleShape)
    .drawWithContent {
        drawContent()
        val marker = 13.dp.toPx()
        val width = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val corners = listOf(
            Offset(0f, 0f) to (Offset(marker, 0f) to Offset(0f, marker)),
            Offset(w, 0f) to (Offset(w - marker, 0f) to Offset(w, marker)),
            Offset(0f, h) to (Offset(marker, h) to Offset(0f, h - marker)),
            Offset(w, h) to (Offset(w - marker, h) to Offset(w, h - marker))
        )
        corners.forEach { (point, ends) ->
            drawLine(accent, point, ends.first, width)
            drawLine(accent, point, ends.second, width)
        }
    }

@Composable
fun PipButton(label: String, accent: Color, modifier: Modifier = Modifier, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) accent else accent.copy(alpha = .13f),
            contentColor = if (active) PipBlack else accent,
            disabledContainerColor = accent.copy(alpha = .04f),
            disabledContentColor = accent.copy(alpha = .35f)
        )
    ) { PipText(label, color = if (active) PipBlack else if (enabled) accent else accent.copy(alpha = .35f), prominent = true, maxLines = 1) }
}

/** A lightweight arc gauge; the progress value is always clamped to [0,1]. */
@Composable
fun PipGauge(label: String, value: Float, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.pipFrame(accent).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PipText(label, color = accent.copy(alpha = .72f), prominent = true)
        Canvas(Modifier.fillMaxWidth().height(100.dp)) {
            val diameter = minOf(size.width * .8f, size.height * 1.6f)
            val left = (size.width - diameter) / 2f
            val top = (size.height - diameter) / 2f + 12.dp.toPx()
            val arcTopLeft = Offset(left, top)
            val arcSize = Size(diameter, diameter)
            drawArc(accent.copy(alpha = .18f), startAngle = 145f, sweepAngle = 250f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(9.dp.toPx(), cap = StrokeCap.Butt))
            drawArc(accent, startAngle = 145f, sweepAngle = 250f * value.coerceIn(0f, 1f), useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(9.dp.toPx(), cap = StrokeCap.Butt))
            for (i in 0..10) {
                val angle = (145f + i * 25f) * PI / 180
                val radius = diameter / 2f
                val center = Offset(size.width / 2f, top + radius)
                val p1 = Offset(center.x + cos(angle).toFloat() * radius * .71f,
                    center.y + sin(angle).toFloat() * radius * .71f)
                val p2 = Offset(center.x + cos(angle).toFloat() * radius * .78f,
                    center.y + sin(angle).toFloat() * radius * .78f)
                drawLine(accent.copy(alpha = .7f), p1, p2, 1.dp.toPx())
            }
        }
        PipText(detail, color = accent, prominent = true)
    }
}

/** Purely decorative overlay. It doesn't create an animation loop or handle pointers. */
fun Modifier.crtScanlines(accent: Color): Modifier = drawWithContent {
    drawContent()
    val gap = 6.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(accent.copy(alpha = .055f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gap
    }
}
