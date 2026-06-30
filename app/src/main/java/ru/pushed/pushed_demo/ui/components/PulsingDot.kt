package ru.pushed.pushed_demo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import ru.pushed.pushed_demo.ui.theme.GreenActive
import ru.pushed.pushed_demo.ui.theme.RedOffline

@Composable
fun PulsingDot(active: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) {
        if (active) {
            val inf = rememberInfiniteTransition(label = "pulse")
            val pulseScale by inf.animateFloat(
                initialValue = 1f, targetValue = 2.4f,
                animationSpec = infiniteRepeatable(
                    tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse
                ), label = "scale"
            )
            val pulseAlpha by inf.animateFloat(
                initialValue = 0.28f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse
                ), label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .scale(pulseScale)
                    .background(GreenActive.copy(alpha = pulseAlpha), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(if (active) GreenActive else RedOffline, CircleShape)
        )
    }
}
