package ru.pushed.pushed_demo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ru.pushed.pushed_demo.ui.theme.Coral

@Composable
fun PushedLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        val s  = size.width / 72f
        val sw = 5.5f * s
        val path = Path().apply {
            moveTo(50f * s, 14f * s)
            lineTo(20f * s, 14f * s)
            quadraticBezierTo(14f * s, 14f * s, 14f * s, 20f * s)
            lineTo(14f * s, 52f * s)
            quadraticBezierTo(14f * s, 58f * s, 20f * s, 58f * s)
            lineTo(52f * s, 58f * s)
            quadraticBezierTo(58f * s, 58f * s, 58f * s, 52f * s)
            lineTo(58f * s, 24f * s)
        }
        drawPath(path, Coral, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Coral, radius = 8.5f * s, center = Offset(58f * s, 14f * s))
    }
}
