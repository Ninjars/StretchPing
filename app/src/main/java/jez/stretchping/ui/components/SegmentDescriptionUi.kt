package jez.stretchping.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerState
import jez.stretchping.features.activetimer.SegmentDescription


@Composable
fun SegmentDescriptionUi(
    state: SegmentDescription,
) {
    val textBackground = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedText(
            text = stringResource(state.mode.toStringRes()),
        ) {
            Text(
                text = it,
                fontSize = 24.sp
            )
        }
        Row {
            AnimatedText(
                text = state.duration.asSeconds().toString(),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 72.sp)) {
                            append(it)
                        }
                        withStyle(SpanStyle(fontSize = 30.sp)) {
                            append("s")
                        }
                    },
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            color = textBackground,
                            radius = this.size.maxDimension / 2f + 8
                        )
                    }
                )
            }
        }
        AnimatedText(
            text = state.repsRemaining,
        ) {
            Text(
                text = it,
                fontSize = 18.sp,
            )
        }
    }
}

@StringRes
private fun ActiveTimerState.Mode.toStringRes(): Int =
    when (this) {
        ActiveTimerState.Mode.Stretch -> R.string.mode_active
        ActiveTimerState.Mode.Transition -> R.string.mode_transition
    }

@Composable
private fun AnimatedText(
    text: String,
    content: @Composable (String) -> Unit
) {
    AnimatedContent(
        label = "animated text",
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(250, delayMillis = 45)) togetherWith
                    fadeOut(animationSpec = tween(90))
        }
    ) {
        content(it)
    }
}
