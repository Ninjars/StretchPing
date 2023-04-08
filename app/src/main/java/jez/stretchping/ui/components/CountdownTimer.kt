package jez.stretchping.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import jez.stretchping.ui.theme.StretchPingTheme
import kotlin.math.max

@Composable
fun CountdownTimer(
    start: Long,
    startAtFraction: Float,
    end: Long,
    pausedAtFraction: Float?,
    modifier: Modifier = Modifier,
) {
    if (pausedAtFraction != null) {
        ArcProgressBar(progress = pausedAtFraction, modifier = modifier)
    } else {
        CountdownTimer(startAtFraction, start, end, modifier)
    }
}

@Composable
private fun CountdownTimer(
    startAtFraction: Float,
    start: Long,
    end: Long,
    modifier: Modifier = Modifier,
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
            animationValue = anim.getValueFromNanos(elapsed * 1000000)
        } while (elapsed <= duration)
    }
    ArcProgressBar(progress = animationValue, modifier = modifier)
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
