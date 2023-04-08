package jez.stretchping.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import jez.stretchping.ui.theme.StretchPingTheme

@Composable
fun CountdownTimer(
    startAtFraction: Float,
    durationMillis: Long,
    pausedAtFraction: Float?,
    modifier: Modifier = Modifier,
) {
    if (pausedAtFraction != null) {
        ArcProgressBar(progress = pausedAtFraction, modifier = modifier)
    } else {
        CountdownTimer(startAtFraction, durationMillis, modifier)
    }
}

@Composable
private fun CountdownTimer(
    startAtFraction: Float,
    durationMillis: Long,
    modifier: Modifier = Modifier,
) {
    val start = remember(startAtFraction, durationMillis) {
        Animatable(startAtFraction)
    }
    LaunchedEffect(start) {
        start.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis.toInt(),
                easing = LinearEasing,
            ),
        )
    }
    ArcProgressBar(progress = start.value, modifier = modifier)
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
