package jez.stretchping.features.activetimer

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
import jez.stretchping.ui.components.ArcProgressBar
import jez.stretchping.ui.components.CountdownTimer
import jez.stretchping.ui.theme.StretchPingTheme
import jez.stretchping.utils.previewState
import jez.stretchping.utils.rememberEventConsumer

@Composable
fun ActiveTimerScreen(
    viewModel: ActiveTimerVM
) {
    ActiveTimerScreen(viewModel.viewState.collectAsState(), rememberEventConsumer(viewModel))
}

@Composable
private fun ActiveTimerScreen(
    state: State<ActiveTimerViewState>,
    eventHandler: (Event) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        MainContent { state.value }
        Spacer(modifier = Modifier.weight(1f))
        Controls(
            eventHandler = eventHandler,
            modifier = Modifier.weight(1f)
        ) { state.value.activeTimer }
    }
}

@Composable
private fun Controls(
    eventHandler: (Event) -> Unit,
    modifier: Modifier = Modifier,
    stateProvider: () -> ActiveTimerState?,
) {
    val timerState = stateProvider()
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(targetState = timerState?.isPaused == true) {
            if (it) {
                CircleButton(
                    onClick = { eventHandler(Event.Reset) },
                    imageVector = Icons.Rounded.Undo,
                    contentDescription = stringResource(id = R.string.timer_resume),
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .aspectRatio(1f),
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .aspectRatio(1f),
                )
            }
        }

        MainButton(
            eventHandler = eventHandler,
            state = timerState,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .aspectRatio(1f),
        )
    }
}

@Composable
private fun MainButton(
    eventHandler: (Event) -> Unit,
    state: ActiveTimerState?,
    modifier: Modifier = Modifier,
) {
    val isPlay = state == null || state.isPaused
    CircleButton(
        onClick = if (isPlay) {
            { eventHandler(Event.Start) }
        } else {
            { eventHandler(Event.Pause) }
        },
        imageVector = when {
            isPlay -> Icons.Rounded.PlayArrow
            else -> Icons.Rounded.Pause
        },
        contentDescription = stringResource(id = R.string.timer_start),
        modifier = modifier,
    )
}

@Composable
private fun CircleButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
    ) {

        Crossfade(targetState = imageVector) {
            Icon(
                imageVector = it,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MainContent(
    stateProvider: () -> ActiveTimerViewState
) {
    val state = stateProvider()
    val activeTimer = state.activeTimer
    if (activeTimer != null) {
        CountdownTimer(
            start = activeTimer.start,
            startAtFraction = activeTimer.startAtFraction,
            end = activeTimer.end,
            pausedAtFraction = activeTimer.pausedAtFraction
        )
    } else {
        ArcProgressBar(progress = 0f)
    }
}

@Preview
@Composable
private fun ActiveTimerScreenPreview() {
    StretchPingTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            ActiveTimerScreen(
                state = previewState {
                    ActiveTimerViewState(
                        activeTimer = ActiveTimerState(
                            start = 0,
                            startAtFraction = 0f,
                            end = 100,
                            pausedAtFraction = 0.33f,
                            mode = ActiveTimerState.Mode.Stretch,
                        )
                    )
                }
            ) {}
        }
    }
}
