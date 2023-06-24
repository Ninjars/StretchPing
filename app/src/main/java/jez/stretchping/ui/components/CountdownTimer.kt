package jez.stretchping.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import jez.stretchping.utils.KeepScreenOn

@Composable
fun CountdownTimer(
    startAtFraction: Float,
    endTimeMillis: Long,
    pausedAtFraction: Float?,
    content: @Composable (Float) -> Unit,
) {
    if (pausedAtFraction != null) {
        content(pausedAtFraction)
    } else {
        CountdownTimer(startAtFraction, endTimeMillis, content)
    }
}

@Composable
private fun CountdownTimer(
    startAtFraction: Float,
    endTimeMillis: Long,
    content: @Composable (Float) -> Unit,
) {
    KeepScreenOn()
    val start = remember(startAtFraction, endTimeMillis) {
        Animatable(startAtFraction)
    }
    LaunchedEffect(start) {
        start.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = (endTimeMillis - System.currentTimeMillis()).toInt(),
                easing = LinearEasing,
            ),
        )
    }
    content(start.value)
}
