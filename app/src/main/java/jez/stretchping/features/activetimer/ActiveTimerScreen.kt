package jez.stretchping.features.activetimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import jez.stretchping.R
import jez.stretchping.features.activetimer.ActiveTimerVM.Event
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
        MainContent(eventHandler) { state.value }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MainContent(
    eventHandler: (Event) -> Unit,
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
        BigPlayButton(eventHandler)
    }
}

@Composable
fun BigPlayButton(eventHandler: (Event) -> Unit) {
    IconButton(
        onClick = { eventHandler(Event.Start) },
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .aspectRatio(1f)
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(id = R.string.timer_start),
            modifier = Modifier.fillMaxSize()
        )
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
