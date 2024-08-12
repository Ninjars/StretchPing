package jez.stretchping.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import dev.redfoxstudio.stretchping.R
import jez.stretchping.features.activetimer.view.ActiveTimerState
import jez.stretchping.features.activetimer.view.ActiveTimerState.Mode.Announce
import jez.stretchping.features.activetimer.view.ActiveTimerState.Mode.Stretch
import jez.stretchping.features.activetimer.view.ActiveTimerState.Mode.Transition
import jez.stretchping.features.activetimer.view.SegmentDescription


@Composable
fun SegmentDescriptionUi(
    state: SegmentDescription,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            if (state.name.isNotBlank()) {
                AnimatedText(
                    text = state.name,
                ) {
                    Text(
                        text = it,
                        fontSize = 24.sp
                    )
                }
            }
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
                )
            }
        }
        AnimatedText(
            text = state.position,
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
        Stretch -> R.string.mode_active
        Transition -> R.string.mode_transition
        Announce -> R.string.mode_announce
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
