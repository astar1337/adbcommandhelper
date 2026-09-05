package utils.helpers

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

const val TWO_PI = (2.0 * PI).toFloat()

@Composable
fun waveFill(
    active: Boolean,
    shape: Shape,
    baseColor: Color
): Modifier {
    if (!active) return Modifier

    val transition = rememberInfiniteTransition(label = "cancelWave")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cancelWavePhase"
    )

    val level by transition.animateFloat(
        initialValue = 0.60f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cancelWaveLevel"
    )

    return Modifier
        .clip(shape)
        .drawBehind {
            drawRect(baseColor.copy(alpha = 0.25f))
            drawWave(phase, level, 1.7f, 0.16f, baseColor.copy(alpha = 0.50f))
            drawWave(phase + 2.1f, level + 0.07f, 1.2f, 0.13f, baseColor)
        }
}

private fun DrawScope.drawWave(
    phase: Float,
    levelFraction: Float,
    cycles: Float,
    amplitudeFraction: Float,
    color: Color
) {
    val w = size.width
    val h = size.height
    val baseline = h * levelFraction
    val amp = h * amplitudeFraction

    val path = Path()
    path.moveTo(0f, h)
    path.lineTo(0f, baseline + sin(phase) * amp)

    var x = 0f
    while (x <= w) {
        path.lineTo(x, baseline + sin(phase + (x / w) * cycles * TWO_PI) * amp)
        x += 2f
    }

    path.lineTo(w, baseline + sin(phase + cycles * TWO_PI) * amp)
    path.lineTo(w, h)
    path.close()

    drawPath(path, color)
}