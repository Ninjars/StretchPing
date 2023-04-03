package jez.stretchping.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jez.stretchping.ui.theme.StretchPingTheme
import kotlin.math.max

private const val ArcStartDeg = 120f
private const val ArcSweepDeg = 300f
private val ArcBackThickness = 6.dp
private val ArcForeThickness = 10.dp

@Composable
fun CountdownTimer(
    start: Long,
    startAtFraction: Float,
    end: Long,
    pausedAtFraction: Float?,
) {
    if (pausedAtFraction != null) {
        ArcProgressBar(progress = pausedAtFraction)
    } else {
        CountdownTimer(startAtFraction, start, end)
    }
}

@Composable
private fun CountdownTimer(
    startAtFraction: Float,
    start: Long,
    end: Long,
) {
    val begin = remember(startAtFraction, end) { System.currentTimeMillis() }
    val duration = max(0, (end - start).toInt())
    val anim = remember(begin) {
        TargetBasedAnimation(
            animationSpec = tween(
                durationMillis = duration,
                easing = LinearEasing,
            ),
            typeConverter = Float.VectorConverter,
            initialValue = startAtFraction,
            targetValue = 1f,
        )
    }
    var animationValue by remember { mutableStateOf(startAtFraction) }
    LaunchedEffect(anim) {
        val firstFrame = withFrameMillis { it }
        do {
            val elapsed = withFrameMillis { it - firstFrame }
            animationValue = startAtFraction + anim.getValueFromNanos(elapsed * 1000000)
        } while (elapsed <= duration)
    }
    ArcProgressBar(progress = animationValue)
}

@Composable
private fun ArcProgressBar(
    progress: Float,
) {
    val arcForeground = MaterialTheme.colors.secondary
    val arcBackground = Color.LightGray

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
    ) {
        // Background Arc
        drawArc(
            color = arcBackground,
            startAngle = ArcStartDeg,
            sweepAngle = ArcSweepDeg,
            useCenter = false,
            topLeft = Offset(ArcForeThickness.toPx(), ArcForeThickness.toPx()),
            style = Stroke(
                width = ArcBackThickness.toPx(),
                cap = StrokeCap.Round,
            )
        )

        // Foreground Arc
        drawArc(
            color = arcForeground,
            startAngle = ArcStartDeg,
            sweepAngle = ArcSweepDeg * progress,
            useCenter = false,
            topLeft = Offset(ArcForeThickness.toPx(), ArcForeThickness.toPx()),
            style = Stroke(
                width = ArcForeThickness.toPx(),
                cap = StrokeCap.Round,
            )
        )
    }
}

@Preview
@Composable
private fun ArcProgressBarPreview() {
    StretchPingTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArcProgressBar(0.33f)
                ArcProgressBar(0.66f)
                ArcProgressBar(1f)
            }
        }
    }
}
